// Unit tests of the hardware independent scanner logic: pio test -e native
#include <unity.h>
#include <cstring>
#include <initializer_list>
#include <tuple>
#include "barcode_reader.h"
#include "direction.h"
#include "feedback_logic.h"
#include "pairing.h"
#include "protocol.h"
#include "scan_queue.h"

using namespace logic;

void setUp() {}
void tearDown() {}

// ---------------------------------------------------------------- direction

/** Runs sensor states {duration ms, a, b} in 2 ms steps; returns the first detected direction. */
static Direction run(DirectionDetector& d, uint32_t& now, std::initializer_list<std::tuple<int, bool, bool>> steps) {
  Direction result = Direction::None;
  for (auto [ms, a, b] : steps) {
    for (int i = 0; i < ms; i += 2) {
      now += 2;
      Direction x = d.update(a, b, now);
      if (x != Direction::None && result == Direction::None) result = x;
    }
  }
  return result;
}

void test_direction_in_and_out() {
  DirectionDetector d;
  uint32_t now = 1000;
  TEST_ASSERT_TRUE(run(d, now, {{100, false, false}, {100, true, false}, {150, true, true}, {100, false, true}, {200, false, false}}) == Direction::In);
  TEST_ASSERT_TRUE(run(d, now, {{100, false, true}, {150, true, true}, {100, true, false}, {200, false, false}}) == Direction::Out);
  TEST_ASSERT_TRUE(run(d, now, {{20, true, false}, {20, true, true}, {20, false, true}, {200, false, false}}) == Direction::In);
}

void test_direction_needs_both_sensors_in_order() {
  DirectionDetector d;
  uint32_t now = 1000;
  TEST_ASSERT_TRUE(run(d, now, {{200, true, false}, {300, false, false}}) == Direction::None);   // only A
  TEST_ASSERT_TRUE(run(d, now, {{200, false, true}, {300, false, false}}) == Direction::None);   // only B
  TEST_ASSERT_TRUE(run(d, now, {{200, true, true}, {300, false, false}}) == Direction::None);    // both at once
  TEST_ASSERT_TRUE(run(d, now, {{100, true, false}, {3000, true, true}, {300, false, false}}) == Direction::None);  // resting
  TEST_ASSERT_TRUE(run(d, now, {{3000, false, true}, {300, false, false}}) == Direction::None);
}

void test_direction_ignores_short_gaps() {
  DirectionDetector d;
  uint32_t now = 1000;
  TEST_ASSERT_TRUE(run(d, now, {{50, true, false}, {4, false, false}, {50, true, true}, {50, false, true}, {200, false, false}}) == Direction::In);
}

void test_invert() {
  TEST_ASSERT_TRUE(applyInvert(Direction::In, true) == Direction::Out);
  TEST_ASSERT_TRUE(applyInvert(Direction::Out, true) == Direction::In);
  TEST_ASSERT_TRUE(applyInvert(Direction::None, true) == Direction::None);
  TEST_ASSERT_TRUE(applyInvert(Direction::In, false) == Direction::In);
}

// ---------------------------------------------------------------- pairing

void test_pairing_in_any_order() {
  ScanPairer p(2000, 1500);
  p.barcode("111", 100);
  TEST_ASSERT_EQUAL(PairResult::None, p.poll(150).kind);
  p.move(Direction::In, 900);
  PairResult r = p.poll(900);
  TEST_ASSERT_EQUAL(PairResult::Submit, r.kind);
  TEST_ASSERT_EQUAL_STRING("111", r.ean.c_str());
  TEST_ASSERT_TRUE(r.direction == Direction::In);

  p.move(Direction::Out, 10000);
  p.barcode("222", 11000);
  r = p.poll(11000);
  TEST_ASSERT_EQUAL(PairResult::Submit, r.kind);
  TEST_ASSERT_TRUE(r.direction == Direction::Out);
}

void test_barcode_without_movement() {
  ScanPairer p(2000, 1500);
  p.move(Direction::In, 0);
  p.barcode("333", 2500);  // movement too old
  TEST_ASSERT_EQUAL(PairResult::None, p.poll(2500).kind);
  PairResult r = p.poll(4600);
  TEST_ASSERT_EQUAL(PairResult::NoDirection, r.kind);
  TEST_ASSERT_EQUAL_STRING("333", r.ean.c_str());
  TEST_ASSERT_EQUAL(PairResult::None, p.poll(5000).kind);
}

void test_movement_without_barcode_is_dropped() {
  ScanPairer p(2000, 1500);
  p.move(Direction::Out, 0);
  TEST_ASSERT_EQUAL(PairResult::None, p.poll(2100).kind);
  p.barcode("444", 2200);
  TEST_ASSERT_EQUAL(PairResult::None, p.poll(2300).kind);  // old movement was dropped
}

void test_same_code_is_ignored_for_a_while() {
  ScanPairer p(2000, 1500);
  p.barcode("555", 0);
  p.move(Direction::In, 10);
  TEST_ASSERT_EQUAL(PairResult::Submit, p.poll(10).kind);
  p.barcode("555", 1000);  // still in view
  p.move(Direction::In, 1010);
  TEST_ASSERT_EQUAL(PairResult::None, p.poll(1010).kind);
  p.barcode("555", 2600);  // next item, same product (more than 1.5 s after the last read)
  p.move(Direction::In, 2610);
  TEST_ASSERT_EQUAL(PairResult::Submit, p.poll(2610).kind);
  p.barcode("666", 2700);  // other code right away
  p.move(Direction::In, 2710);
  TEST_ASSERT_EQUAL(PairResult::Submit, p.poll(2710).kind);
}

// ---------------------------------------------------------------- barcode reader

void test_reader_frames_codes() {
  BarcodeReader r;
  std::string out;
  TEST_ASSERT_FALSE(r.feed('\r', 0, out));  // empty line
  for (char c : std::string("40063\x01" "8\x7f" "1")) TEST_ASSERT_FALSE(r.feed(c, 1, out));
  TEST_ASSERT_TRUE(r.feed('\n', 2, out));
  TEST_ASSERT_EQUAL_STRING("4006381", out.c_str());

  TEST_ASSERT_FALSE(r.feed('7', 100, out));
  TEST_ASSERT_FALSE(r.poll(120, out));
  TEST_ASSERT_TRUE(r.poll(151, out));
  TEST_ASSERT_EQUAL_STRING("7", out.c_str());
  TEST_ASSERT_FALSE(r.poll(500, out));

  for (int i = 0; i < 100; i++) r.feed('9', 600, out);
  TEST_ASSERT_TRUE(r.feed('\r', 601, out));
  TEST_ASSERT_EQUAL(BarcodeReader::kMaxLength, out.size());
}

// ---------------------------------------------------------------- feedback

void test_signal_for_result() {
  TEST_ASSERT_TRUE(signalForResult("IN_OK") == Signal::InOk);
  TEST_ASSERT_TRUE(signalForResult("IN_OK_NEW_ARTICLE") == Signal::InOk);
  TEST_ASSERT_TRUE(signalForResult("IN_OK_UNKNOWN_ARTICLE") == Signal::InUnknown);
  TEST_ASSERT_TRUE(signalForResult("OUT_OK") == Signal::OutOk);
  TEST_ASSERT_TRUE(signalForResult("PENDING_ARTICLE") == Signal::Waiting);
  TEST_ASSERT_TRUE(signalForResult("NOT_IN_STOCK") == Signal::Error);
}

void test_feedback_timer() {
  FeedbackTimer t;
  TEST_ASSERT_FALSE(t.timedOut(100000));  // nothing awaited
  t.sent(1, 0);
  TEST_ASSERT_FALSE(t.timedOut(5000));
  TEST_ASSERT_TRUE(t.timedOut(5001));
  TEST_ASSERT_FALSE(t.timedOut(9000));    // reported once

  t.sent(2, 10000);
  TEST_ASSERT_TRUE(t.received(2, "PENDING_ARTICLE", 12000) == Signal::Waiting);
  TEST_ASSERT_FALSE(t.timedOut(30000));   // extended wait
  TEST_ASSERT_TRUE(t.received(99, "OUT_OK", 30500) == Signal::OutOk);  // answer to a queued scan
  TEST_ASSERT_TRUE(t.received(2, "IN_OK_UNKNOWN_ARTICLE", 31000) == Signal::InUnknown);
  TEST_ASSERT_FALSE(t.timedOut(60000));

  t.sent(3, 70000);
  TEST_ASSERT_TRUE(t.received(3, "PENDING_ARTICLE", 70000) == Signal::Waiting);
  TEST_ASSERT_TRUE(t.timedOut(90001));
}

// ---------------------------------------------------------------- queue

void test_queue() {
  ScanQueue q;
  TEST_ASSERT_TRUE(q.empty());
  q.pop();  // no-op
  for (uint32_t i = 1; i <= ScanQueue::kCapacity; i++) TEST_ASSERT_TRUE(q.push({std::to_string(i), Direction::In, i}));
  TEST_ASSERT_FALSE(q.push({"x", Direction::Out, 99}));
  TEST_ASSERT_EQUAL(ScanQueue::kCapacity, q.size());
  TEST_ASSERT_EQUAL(1, q.front().seq);
  q.pop();
  TEST_ASSERT_TRUE(q.push({"33", Direction::Out, 33}));  // wraps around
  for (uint32_t i = 2; i <= ScanQueue::kCapacity; i++) q.pop();
  TEST_ASSERT_EQUAL(33, q.front().seq);
  TEST_ASSERT_TRUE(q.front().direction == Direction::Out);
}

// ---------------------------------------------------------------- protocol

void test_protocol() {
  TEST_ASSERT_EQUAL_STRING("{\"ean\":\"4006381333931\",\"direction\":\"IN\",\"seq\":7,\"bootId\":\"ab12\"}",
                           scanMessage("4006381333931", Direction::In, 7, "ab12").c_str());
  TEST_ASSERT_EQUAL_STRING("{\"ean\":\"1\",\"direction\":\"OUT\",\"seq\":1,\"bootId\":\"x\"}",
                           scanMessage("1", Direction::Out, 1, "x").c_str());
  TEST_ASSERT_EQUAL_STRING("{\"online\":true,\"fw\":\"0.2.0\",\"ip\":\"10.0.0.5\",\"rssi\":-60,\"bootId\":\"b\"}",
                           statusMessage("0.2.0", "10.0.0.5", -60, "b").c_str());

  Feedback f;
  const char* ok = "{\"seq\":7,\"result\":\"IN_OK\",\"success\":true,\"article\":\"Reis\",\"quantity\":2}";
  TEST_ASSERT_TRUE(parseFeedback(ok, strlen(ok), f));
  TEST_ASSERT_EQUAL(7, f.seq);
  TEST_ASSERT_EQUAL_STRING("IN_OK", f.result.c_str());
  TEST_ASSERT_EQUAL_STRING("Reis", f.article.c_str());

  const char* minimal = "{}";
  TEST_ASSERT_TRUE(parseFeedback(minimal, strlen(minimal), f));
  TEST_ASSERT_EQUAL(0, f.seq);
  TEST_ASSERT_EQUAL_STRING("ERROR", f.result.c_str());
  TEST_ASSERT_EQUAL_STRING("", f.article.c_str());

  const char* broken = "{\"seq\":";
  TEST_ASSERT_FALSE(parseFeedback(broken, strlen(broken), f));
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_direction_in_and_out);
  RUN_TEST(test_direction_needs_both_sensors_in_order);
  RUN_TEST(test_direction_ignores_short_gaps);
  RUN_TEST(test_invert);
  RUN_TEST(test_pairing_in_any_order);
  RUN_TEST(test_barcode_without_movement);
  RUN_TEST(test_movement_without_barcode_is_dropped);
  RUN_TEST(test_same_code_is_ignored_for_a_while);
  RUN_TEST(test_reader_frames_codes);
  RUN_TEST(test_signal_for_result);
  RUN_TEST(test_feedback_timer);
  RUN_TEST(test_queue);
  RUN_TEST(test_protocol);
  return UNITY_END();
}
