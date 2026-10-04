package com.finalproject;

import com.finalproject.config.MomoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class MomoConfigTests {
    @Test void mockNeedsNoKeysButSandboxRequiresOwnKeysAndNeverAllowsProductionEndpoint() {
        MomoConfig c=new MomoConfig();
        ReflectionTestUtils.setField(c,"mode","mock");
        assertThatCode(c::validate).doesNotThrowAnyException();
        ReflectionTestUtils.setField(c,"mode","sandbox");
        ReflectionTestUtils.setField(c,"partnerCode",""); ReflectionTestUtils.setField(c,"accessKey",""); ReflectionTestUtils.setField(c,"secretKey","");
        assertThatThrownBy(c::validate).hasMessageContaining("bộ khóa test");
        ReflectionTestUtils.setField(c,"partnerCode","TEST"); ReflectionTestUtils.setField(c,"accessKey","TEST"); ReflectionTestUtils.setField(c,"secretKey","TEST");
        ReflectionTestUtils.setField(c,"endpoint","https://payment.momo.vn/v2/gateway/api/create");
        assertThatThrownBy(c::validate).hasMessageContaining("MoMo Test");
        ReflectionTestUtils.setField(c,"mode","production");
        assertThatThrownBy(c::validate).hasMessageContaining("project dev");
    }
}
