package com.mycompany.myapp.config;

import com.mycompany.myapp.domain.Branch;
import com.mycompany.myapp.domain.Itinerary;
import com.mycompany.myapp.repository.BranchRepository;
import com.mycompany.myapp.repository.ItineraryRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures Branch/Itinerary master rows exist when Liquibase seed did not populate them.
 * Runs on dev/demo/prod — only fills when {@code branch} count is 0 (idempotent).
 */
@Component
@Profile({ "dev", "demo", "prod" })
@Order(110)
public class DemoBranchItinerarySeed implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoBranchItinerarySeed.class);

    private final BranchRepository branchRepository;
    private final ItineraryRepository itineraryRepository;

    public DemoBranchItinerarySeed(BranchRepository branchRepository, ItineraryRepository itineraryRepository) {
        this.branchRepository = branchRepository;
        this.itineraryRepository = itineraryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (branchRepository.count() > 0) {
            return;
        }
        LOG.warn("branch table empty — seeding Branch/Itinerary from classpath CSV");
        Map<Long, Branch> bySeedId = new HashMap<>();
        try (
            BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    new ClassPathResource("config/liquibase/seed-data/branch.csv").getInputStream(),
                    StandardCharsets.UTF_8
                )
            )
        ) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] p = line.split(";", -1);
                Branch b = new Branch();
                b.setCode(p[1]);
                b.setName(p[2]);
                b.setActive(Boolean.parseBoolean(p[3]));
                b = branchRepository.save(b);
                bySeedId.put(Long.parseLong(p[0]), b);
            }
        }
        try (
            BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    new ClassPathResource("config/liquibase/seed-data/itinerary.csv").getInputStream(),
                    StandardCharsets.UTF_8
                )
            )
        ) {
            String line = br.readLine();
            int n = 0;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] p = line.split(";", -1);
                Branch branch = bySeedId.get(Long.parseLong(p[3]));
                if (branch == null) {
                    LOG.warn("Skip itinerary {} — unknown branch_id {}", p[1], p[3]);
                    continue;
                }
                Itinerary it = new Itinerary();
                it.setCode(p[1]);
                it.setName(p[2]);
                it.setBranch(branch);
                it.setDeparturePoint(emptyToNull(p[4]));
                it.setDestinationPoint(emptyToNull(p[5]));
                it.setRouteDirection(emptyToNull(p[6]));
                it.setRouteType(parseInt(p[7]));
                it.setPrice(parseDecimal(p[8]));
                it.setPriority(parseInt(p[9]));
                it.setDisplayOrder(parseInt(p[10]));
                it.setActive(Boolean.parseBoolean(p[11]));
                it.setShortestItinerary(emptyToNull(p.length > 12 ? p[12] : null));
                itineraryRepository.save(it);
                n++;
            }
            LOG.info("Seeded {} branches and {} itineraries", bySeedId.size(), n);
        }
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        return Integer.valueOf(s);
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        return new BigDecimal(s);
    }
}
