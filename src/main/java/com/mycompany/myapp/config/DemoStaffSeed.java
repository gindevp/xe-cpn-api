package com.mycompany.myapp.config;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.StaffProfile;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.RoleCode;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.OfficeRepository;
import com.mycompany.myapp.repository.StaffProfileRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds demo staff users aligned with FE {@code MOCK_USERS} (password {@code 123}).
 * Profiles: {@code dev} (local) and {@code demo} (Railway/UAT with {@code prod,demo}).
 * Idempotent — safe on restart. Do not enable {@code demo} on hardened production long-term
 * without rotating passwords.
 */
@Component
@Profile({ "dev", "demo" })
@Order(100)
public class DemoStaffSeed implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoStaffSeed.class);
    private static final String DEMO_PASSWORD = "123";
    private static final String DEFAULT_HOME_OFFICE = "GP";

    private record DemoStaff(String login, RoleCode role, String officeCode, boolean scopeAll, String displayName) {}

    /** Only three job titles remain: Admin, Điều phối, Kế toán. Office scope is per account. */
    private static final List<DemoStaff> DEMO = List.of(
        new DemoStaff("admin", RoleCode.AD, DEFAULT_HOME_OFFICE, true, "Administrator"),
        new DemoStaff("quay.hn", RoleCode.DH, "GP", false, "Điều phối HN"),
        new DemoStaff("quay.hcm", RoleCode.DH, "NB", false, "Điều phối NB"),
        new DemoStaff("bx.hn", RoleCode.DH, "GP", false, "Điều phối kho HN"),
        new DemoStaff("giao.hn.01", RoleCode.DH, "GP", false, "Giao HN 01"),
        new DemoStaff("kt.hn", RoleCode.KT, "GP", false, "Kế toán HN"),
        new DemoStaff("tcn.hn", RoleCode.DH, "GP", false, "Điều phối CN HN"),
        new DemoStaff("dh", RoleCode.DH, DEFAULT_HOME_OFFICE, true, "Điều phối toàn hệ thống"),
        new DemoStaff("bl", RoleCode.DH, DEFAULT_HOME_OFFICE, true, "Điều phối giám sát")
    );

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final OfficeRepository officeRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoStaffSeed(
        UserRepository userRepository,
        AuthorityRepository authorityRepository,
        OfficeRepository officeRepository,
        StaffProfileRepository staffProfileRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.officeRepository = officeRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (officeRepository.count() == 0) {
            LOG.warn("Skip DemoStaffSeed: no offices (run Liquibase seed first)");
            return;
        }
        Authority userAuthority = authorityRepository
            .findById(AuthoritiesConstants.USER)
            .orElseThrow(() -> new IllegalStateException("ROLE_USER missing"));
        Authority adminAuthority = authorityRepository.findById(AuthoritiesConstants.ADMIN).orElse(null);

        for (DemoStaff demo : DEMO) {
            ensureUser(demo, userAuthority, adminAuthority);
            ensureStaffProfile(demo);
        }
        LOG.info("Demo staff seed ready ({} profiles, password '{}')", DEMO.size(), DEMO_PASSWORD);
    }

    private void ensureUser(DemoStaff demo, Authority userAuthority, Authority adminAuthority) {
        userRepository
            .findOneByLogin(demo.login().toLowerCase())
            .ifPresentOrElse(
                existing -> {
                    boolean dirty = false;
                    if (
                        demo.role() == RoleCode.AD &&
                        adminAuthority != null &&
                        existing.getAuthorities().stream().noneMatch(a -> AuthoritiesConstants.ADMIN.equals(a.getName()))
                    ) {
                        existing.getAuthorities().add(adminAuthority);
                        dirty = true;
                    }
                    // Align password with FE MOCK_USERS the first time we attach a StaffProfile
                    if (staffProfileRepository.findOneByUserLoginIgnoreCase(demo.login()).isEmpty()) {
                        existing.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
                        dirty = true;
                    }
                    if (dirty) {
                        userRepository.save(existing);
                    }
                },
                () -> {
                    User user = new User();
                    user.setLogin(demo.login().toLowerCase());
                    user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
                    user.setFirstName(demo.displayName());
                    user.setLastName(demo.role().name());
                    user.setEmail(demo.login().toLowerCase().replace('.', '_') + "@localhost");
                    user.setActivated(true);
                    user.setLangKey("vi");
                    Set<Authority> authorities = new HashSet<>();
                    authorities.add(userAuthority);
                    if (demo.role() == RoleCode.AD && adminAuthority != null) {
                        authorities.add(adminAuthority);
                    }
                    user.setAuthorities(authorities);
                    userRepository.save(user);
                    LOG.debug("Created demo user {}", demo.login());
                }
            );
    }

    private void ensureStaffProfile(DemoStaff demo) {
        if (staffProfileRepository.findOneByUserLoginIgnoreCase(demo.login()).isPresent()) {
            return;
        }
        String officeCode = demo.scopeAll() ? DEFAULT_HOME_OFFICE : demo.officeCode();
        Office office = officeRepository
            .findOneByCode(officeCode)
            .orElseThrow(() -> new IllegalStateException("Office not found: " + officeCode));

        StaffProfile profile = new StaffProfile();
        profile.setStaffCode("STF-" + demo.role().name() + "-" + demo.login().toUpperCase().replace('.', '-'));
        profile.setUserLogin(demo.login().toLowerCase());
        profile.setDisplayName(demo.displayName());
        profile.setRoleCode(demo.role());
        profile.setScopeAllOffices(demo.scopeAll());
        profile.setActive(true);
        profile.setOffice(office);
        staffProfileRepository.save(profile);
        LOG.debug("Created staff profile for {}", demo.login());
    }
}
