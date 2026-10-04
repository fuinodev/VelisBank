package com.velisbank;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
class LoginAttemptsTest {
    static class TestClock extends Clock {
        long now=1000;
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return Instant.ofEpochMilli(now);}
    }
    @Test void threeFailuresBlockForExactlyFiveMinutesAndSuccessResetsCounter() {
        var clock=new TestClock();var limiter=new LoginAttempts(clock);var a=limiter.entry(" User ");
        assertThat(limiter.entry("user")).isSameAs(a);
        assertThat(limiter.failed(a)).isEqualTo(2);assertThat(limiter.failed(a)).isEqualTo(1);
        assertThat(limiter.remaining(a)).isZero();assertThat(limiter.failed(a)).isZero();
        assertThat(limiter.remaining(a)).isEqualTo(300);
        clock.now+=299_000;assertThat(limiter.remaining(a)).isEqualTo(1);
        clock.now+=1000;assertThat(limiter.remaining(a)).isZero();
        assertThat(limiter.failed(a)).isEqualTo(2);limiter.success(a);assertThat(limiter.failed(a)).isEqualTo(2);
    }
}
