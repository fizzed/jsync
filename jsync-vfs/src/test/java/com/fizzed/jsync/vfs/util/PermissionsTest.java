package com.fizzed.jsync.vfs.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PermissionsTest {

    @Test
    public void mergeOwnerPermissions() {
        int source = 0755;
        int target = 0640;
        int result = Permissions.mergeOwnerPermissions(source, target);

        assertThat(result).isEqualTo(0740);
    }

    @Test
    public void isOwnerPermissionEqual() {
        assertThat(Permissions.isOwnerPermissionEqual(0755, 0655)).isFalse();
        assertThat(Permissions.isOwnerPermissionEqual(0755, 0744)).isTrue();
        assertThat(Permissions.isOwnerPermissionEqual(01755, 0744)).isTrue();
    }

}