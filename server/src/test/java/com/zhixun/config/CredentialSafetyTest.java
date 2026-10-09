package com.zhixun.config;

import com.zhixun.common.util.AesUtil;
import com.zhixun.mapper.UserMapper;
import com.zhixun.entity.User;
import com.zhixun.enums.RoleEnum;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.mockito.Mockito.*;

class CredentialSafetyTest {
  @Test void missingAdminCredentialsDoNotCreateAnAccount() {

    var mapper = mock(UserMapper.class);
    var encoder = mock(PasswordEncoder.class);
    var aes = mock(AesUtil.class);
    new DefaultUserDataInitializer(mapper, encoder, aes).initializeAdmin("", "");
    verifyNoInteractions(mapper, encoder, aes);
  }
  @Test void existingValidAdminPasswordIsNeverOverwritten() {
    var mapper = mock(UserMapper.class);
    var encoder = mock(PasswordEncoder.class);
    var user = new User(); user.setId(1L); user.setUid("existing-user");
    user.setRole(RoleEnum.SUPER_ADMIN); user.setPasswordHash("existing-hash");
    when(mapper.selectOne(any())).thenReturn(user);
    new DefaultUserDataInitializer(mapper, encoder, mock(AesUtil.class))
        .initializeAdmin("operator", "test-only-input-not-a-credential");
    verifyNoInteractions(encoder);
    verify(mapper, never()).updateById(any(User.class));
  }
  @Test void disabledAdminSeedRequiresExplicitPasswordToActivate() {
    var mapper = mock(UserMapper.class);
    var encoder = mock(PasswordEncoder.class);
    var user = new User(); user.setId(1L); user.setUid("existing-user");
    user.setRole(RoleEnum.SUPER_ADMIN); user.setPasswordHash("");
    when(mapper.selectOne(any())).thenReturn(user);
    when(encoder.encode(anyString())).thenReturn("synthetic-hash");
    new DefaultUserDataInitializer(mapper, encoder, mock(AesUtil.class))
        .initializeAdmin("operator", "test-only-input-not-a-credential");
    verify(mapper).updateById(argThat((User update) -> "synthetic-hash".equals(update.getPasswordHash())));
  }
  @Test void missingAesKeyFailsRatherThanEncryptingWithAnEmptyKey() {
    var aes = new AesUtil();
    ReflectionTestUtils.setField(aes, "secretKey", "");
    assertThrows(IllegalStateException.class, aes::validateSecretKey);
  }
}
