package com.aicte.fdrportal.config;

import com.aicte.fdrportal.entity.UserAccount;
import com.aicte.fdrportal.enums.UserRole;
import com.aicte.fdrportal.repository.UserAccountRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BootstrapAdminInitializer implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public BootstrapAdminInitializer(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.email:}") String adminEmail,
            @Value("${app.bootstrap.admin.password:}") String adminPassword) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn(
                    "Bootstrap admin not configured. "
                            + "Set BOOTSTRAP_ADMIN_EMAIL and "
                            + "BOOTSTRAP_ADMIN_PASSWORD in your environment.");
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
                existing -> {
                    if (existing.getRole() != UserRole.ADMIN) {
                        throw new IllegalStateException(
                                "Bootstrap email already exists "
                                        + "with a non-admin role");
                    }
                    log.info("Bootstrap administrator already exists.");
                },
                () -> {
                    UserAccount admin = new UserAccount();
                    admin.setEmail(email);
                    admin.setPasswordHash(
                            passwordEncoder.encode(adminPassword));
                    admin.setRole(UserRole.ADMIN);
                    admin.setEnabled(true);

                    userRepository.save(admin);

                    log.info("Bootstrap administrator created.");
                }
        );
    }
}