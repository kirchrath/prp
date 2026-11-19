package home.inventory.device.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GtinTest {

    @ParameterizedTest
    @CsvSource({
            "4006381333931, 4006381333931",      // EAN-13
            "' 4006381333931 ', 4006381333931",  // trimmed
            "96385074, 96385074",                // EAN-8
            "036000291452, 0036000291452",       // UPC-A -> EAN-13
            "04006381333931, 4006381333931",     // GTIN-14 with leading 0 -> EAN-13
            "14006381333938, 14006381333938",    // GTIN-14 (packaging level 1)
            "01234505, 0012000003455",           // UPC-E, last digit 0..2
            "01234133, 0012300000413",           // UPC-E, last digit 3
            "01234543, 0012340000053",           // UPC-E, last digit 4
            "11234502, 0112000003452",           // UPC-E, number system 1
    })
    void normalizesValidCodes(String raw, String expected) {
        assertThat(Gtin.normalize(raw)).contains(expected);
        assertThat(Gtin.require(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",                 // empty
            "4006381333932",    // wrong check digit
            "400638133393A",    // letter
            "4006-381333931",   // other character
            "1234567",          // too short
            "123456789",        // 9 digits
            "123456789012345",  // too long
            "036000291453",     // UPC-A with wrong check digit
            "24006381333931",   // GTIN-14 with wrong check digit
            "21234505",         // 8 digits, neither EAN-8 nor UPC-E (number system 2)
            "01234506",         // UPC-E with wrong check digit
            "01234561",         // UPC-E (last digit 5..9) with wrong check digit
    })
    void rejectsInvalidCodes(String raw) {
        assertThat(Gtin.normalize(raw)).isEqualTo(Optional.empty());
        assertThatThrownBy(() -> Gtin.require(raw))
                .isInstanceOf(InvalidGtinException.class)
                .hasMessageStartingWith(Gtin.INVALID);
    }
}
