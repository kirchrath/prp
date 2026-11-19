#!/usr/bin/env bash
# Plays an ESP32 scanner or button box via MQTT (needs mosquitto_pub/mosquitto_sub, "apt install mosquitto-clients").
#   dev/simulate-scanner.sh status                  register the scanner (then assign a location in the UI)
#   dev/simulate-scanner.sh in  4006381333931       store one unit
#   dev/simulate-scanner.sh out 4006381333931       remove one unit
#   DEVICE=knopf-dev dev/simulate-scanner.sh buttons    register a button box (assign location + articles in the UI)
#   DEVICE=knopf-dev dev/simulate-scanner.sh press 3    short press of button 3 (one unit in)
#   DEVICE=knopf-dev dev/simulate-scanner.sh hold 3     long press of button 3 (one unit out)
# Environment: MQTT_HOST (localhost), MQTT_PORT (1883), DEVICE (scanner-dev), MQTT_USER/MQTT_PASSWORD (optional)
set -euo pipefail
host=${MQTT_HOST:-localhost}; port=${MQTT_PORT:-1883}; device=${DEVICE:-scanner-dev}
auth=()
[[ -n "${MQTT_USER:-}" ]] && auth=(-u "$MQTT_USER" -P "${MQTT_PASSWORD:-}")
prefix="inventory/scanner/$device"

case "${1:-}" in
  status)
    mosquitto_pub -h "$host" -p "$port" "${auth[@]}" -t "$prefix/status" -r \
      -m "{\"online\":true,\"fw\":\"simulator\",\"ip\":\"127.0.0.1\",\"bootId\":\"sim\"}"
    echo "Scanner $device registered - assign a location under Einstellungen -> Geräte"
    ;;
  buttons)
    mosquitto_pub -h "$host" -p "$port" "${auth[@]}" -t "$prefix/status" -r \
      -m "{\"online\":true,\"fw\":\"simulator\",\"ip\":\"127.0.0.1\",\"bootId\":\"sim\",\"kind\":\"buttons\"}"
    echo "Button box $device registered - assign a location and articles under Einstellungen -> Geräte"
    ;;
  press|hold)
    button=${2:?button number (1-8) missing}
    boot="sim-$(date +%s%N)"
    direction=$([[ "$1" == press ]] && echo IN || echo OUT)
    ( sleep 0.5; mosquitto_pub -h "$host" -p "$port" "${auth[@]}" -t "$prefix/button" -q 1 \
        -m "{\"button\":$button,\"direction\":\"$direction\",\"seq\":1,\"bootId\":\"$boot\"}" ) &
    mosquitto_sub -h "$host" -p "$port" "${auth[@]}" -t "$prefix/feedback" -C 1 -W 25 || { echo "no answer within 25 s"; exit 1; }
    ;;
  in|out)
    ean=${2:?EAN missing}
    # a new bootId per call, so the duplicate detection (bootId + seq) never drops a simulated scan
    boot="sim-$(date +%s%N)"
    direction=$(echo "$1" | tr '[:lower:]' '[:upper:]')
    sub=(mosquitto_sub -h "$host" -p "$port" "${auth[@]}" -t "$prefix/feedback" -C 1 -W 25)
    ( sleep 0.5; mosquitto_pub -h "$host" -p "$port" "${auth[@]}" -t "$prefix/scan" -q 1 \
        -m "{\"ean\":\"$ean\",\"direction\":\"$direction\",\"seq\":1,\"bootId\":\"$boot\"}" ) &
    answer=$("${sub[@]}") || { echo "no answer within 25 s"; exit 1; }
    echo "$answer"
    # unknown barcode: the article is being created, the final result follows
    if [[ "$answer" == *PENDING_ARTICLE* ]]; then "${sub[@]}" || echo "no final answer within 25 s"; fi
    ;;
  *) sed -n '2,10p' "$0"; exit 1 ;;
esac
