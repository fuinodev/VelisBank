package com.velisbank;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MobileNumbersTest {
    @Test void equivalentFormatsHaveOneIdentity() {
        for (String number : new String[]{"0917 123 4567", "9171234567", "639171234567", "+63 (917) 123-4567"}) {
            assertThat(MobileNumbers.normalize(number)).isEqualTo("+639171234567");
        }
    }
    @Test void rejectsLandlinesMalformedAndUnsupportedNumbers() {
        for (String number : new String[]{"", "00000000000", "0281234567", "+97412345678", "+6391712345678", "0917abc4567", "++639171234567"}) {
            assertThatThrownBy(() -> MobileNumbers.normalize(number)).isInstanceOf(BankException.class);
        }
        assertThatThrownBy(() -> MobileNumbers.normalize(null)).isInstanceOf(BankException.class);
    }
}
