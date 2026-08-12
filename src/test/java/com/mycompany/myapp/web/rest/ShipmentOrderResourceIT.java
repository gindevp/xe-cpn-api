package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ShipmentOrderAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Customer;
import com.mycompany.myapp.domain.Office;
import com.mycompany.myapp.domain.OrderFareAdjustmentRequest;
import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.OrderReturnRequest;
import com.mycompany.myapp.domain.ShipmentOrder;
import com.mycompany.myapp.domain.Trip;
import com.mycompany.myapp.domain.enumeration.ForwardStage;
import com.mycompany.myapp.domain.enumeration.GoodsType;
import com.mycompany.myapp.domain.enumeration.OrderStatus;
import com.mycompany.myapp.domain.enumeration.PaymentTerm;
import com.mycompany.myapp.domain.enumeration.ReturnStage;
import com.mycompany.myapp.domain.enumeration.ServiceType;
import com.mycompany.myapp.repository.ShipmentOrderRepository;
import com.mycompany.myapp.service.ShipmentOrderService;
import com.mycompany.myapp.service.dto.ShipmentOrderDTO;
import com.mycompany.myapp.service.mapper.ShipmentOrderMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ShipmentOrderResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ShipmentOrderResourceIT {

    private static final String DEFAULT_ORDER_CODE = "AAAAAAAAAA";
    private static final String UPDATED_ORDER_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_DRAFT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_DRAFT_CODE = "BBBBBBBBBB";

    private static final OrderStatus DEFAULT_STATUS = OrderStatus.DRAFT;
    private static final OrderStatus UPDATED_STATUS = OrderStatus.CONFIRMED;

    private static final ForwardStage DEFAULT_FORWARD_STAGE = ForwardStage.PICKED;
    private static final ForwardStage UPDATED_FORWARD_STAGE = ForwardStage.WH_IN;

    private static final ReturnStage DEFAULT_RETURN_STAGE = ReturnStage.RETURN_PENDING;
    private static final ReturnStage UPDATED_RETURN_STAGE = ReturnStage.RT_TRANSFER_PENDING;

    private static final PaymentTerm DEFAULT_PAYMENT_TERM = PaymentTerm.GUI_TRA;
    private static final PaymentTerm UPDATED_PAYMENT_TERM = PaymentTerm.NHAN_TRA;

    private static final GoodsType DEFAULT_GOODS_TYPE = GoodsType.THUONG;
    private static final GoodsType UPDATED_GOODS_TYPE = GoodsType.DE_VO;

    private static final ServiceType DEFAULT_SERVICE_TYPE = ServiceType.COUNTER_TO_COUNTER;
    private static final ServiceType UPDATED_SERVICE_TYPE = ServiceType.COUNTER_TO_HOME;

    private static final String DEFAULT_SENDER_NAME = "AAAAAAAAAA";
    private static final String UPDATED_SENDER_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SENDER_PHONE = "+84303478052";
    private static final String UPDATED_SENDER_PHONE = "0528269626";

    private static final String DEFAULT_RECEIVER_NAME = "AAAAAAAAAA";
    private static final String UPDATED_RECEIVER_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_RECEIVER_PHONE = "+84837835844";
    private static final String UPDATED_RECEIVER_PHONE = "+84572408319";

    private static final String DEFAULT_DELIVERY_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_DELIVERY_ADDRESS = "BBBBBBBBBB";

    private static final String DEFAULT_PICKUP_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_PICKUP_ADDRESS = "BBBBBBBBBB";

    private static final Boolean DEFAULT_HOME_PICKUP = false;
    private static final Boolean UPDATED_HOME_PICKUP = true;

    private static final Boolean DEFAULT_HOME_DELIVERY = false;
    private static final Boolean UPDATED_HOME_DELIVERY = true;

    private static final Boolean DEFAULT_QR_DROP_OFF = false;
    private static final Boolean UPDATED_QR_DROP_OFF = true;

    private static final String DEFAULT_PICKUP_STAFF_USERNAME = "AAAAAAAAAA";
    private static final String UPDATED_PICKUP_STAFF_USERNAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_PICKING_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PICKING_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_PICKED_UP_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PICKED_UP_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_RECEIVER_ACTUAL_NAME = "AAAAAAAAAA";
    private static final String UPDATED_RECEIVER_ACTUAL_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_RECEIVER_ACTUAL_PHONE = "AAAAAAAAAA";
    private static final String UPDATED_RECEIVER_ACTUAL_PHONE = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_WEIGHT_KG = new BigDecimal(0);
    private static final BigDecimal UPDATED_WEIGHT_KG = new BigDecimal(1);
    private static final BigDecimal SMALLER_WEIGHT_KG = new BigDecimal(0 - 1);

    private static final Integer DEFAULT_QUANTITY = 1;
    private static final Integer UPDATED_QUANTITY = 2;
    private static final Integer SMALLER_QUANTITY = 1 - 1;

    private static final String DEFAULT_DIMENSIONS_TEXT = "AAAAAAAAAA";
    private static final String UPDATED_DIMENSIONS_TEXT = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_FARE_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_FARE_AMOUNT = new BigDecimal(1);
    private static final BigDecimal SMALLER_FARE_AMOUNT = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_PICKUP_FEE_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_PICKUP_FEE_AMOUNT = new BigDecimal(1);
    private static final BigDecimal SMALLER_PICKUP_FEE_AMOUNT = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_DELIVERY_FEE_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_DELIVERY_FEE_AMOUNT = new BigDecimal(1);
    private static final BigDecimal SMALLER_DELIVERY_FEE_AMOUNT = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_PARTNER_FEE_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_PARTNER_FEE_AMOUNT = new BigDecimal(1);
    private static final BigDecimal SMALLER_PARTNER_FEE_AMOUNT = new BigDecimal(0 - 1);

    private static final BigDecimal DEFAULT_PAID_AMOUNT = new BigDecimal(0);
    private static final BigDecimal UPDATED_PAID_AMOUNT = new BigDecimal(1);
    private static final BigDecimal SMALLER_PAID_AMOUNT = new BigDecimal(0 - 1);

    private static final Integer DEFAULT_SHELF_NUMBER = 0;
    private static final Integer UPDATED_SHELF_NUMBER = 1;
    private static final Integer SMALLER_SHELF_NUMBER = 0 - 1;

    private static final String DEFAULT_NOTE = "AAAAAAAAAA";
    private static final String UPDATED_NOTE = "BBBBBBBBBB";

    private static final String DEFAULT_CANCEL_REASON = "AAAAAAAAAA";
    private static final String UPDATED_CANCEL_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_LABEL_PRINTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LABEL_PRINTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_LABEL_REPRINT_COUNT = 0;
    private static final Integer UPDATED_LABEL_REPRINT_COUNT = 1;
    private static final Integer SMALLER_LABEL_REPRINT_COUNT = 0 - 1;

    private static final Integer DEFAULT_FAIL_COUNT = 0;
    private static final Integer UPDATED_FAIL_COUNT = 1;
    private static final Integer SMALLER_FAIL_COUNT = 0 - 1;

    private static final String DEFAULT_PARTNER_CODE = "AAAAAAAAAA";
    private static final String UPDATED_PARTNER_CODE = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_PAYMENT_PERCENT = new BigDecimal(0);
    private static final BigDecimal UPDATED_PAYMENT_PERCENT = new BigDecimal(1);
    private static final BigDecimal SMALLER_PAYMENT_PERCENT = new BigDecimal(0 - 1);

    private static final Boolean DEFAULT_PUBLIC_TRACKING_ALLOWED = false;
    private static final Boolean UPDATED_PUBLIC_TRACKING_ALLOWED = true;

    private static final String ENTITY_API_URL = "/api/shipment-orders";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ShipmentOrderRepository shipmentOrderRepository;

    @Mock
    private ShipmentOrderRepository shipmentOrderRepositoryMock;

    @Autowired
    private ShipmentOrderMapper shipmentOrderMapper;

    @Mock
    private ShipmentOrderService shipmentOrderServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restShipmentOrderMockMvc;

    private ShipmentOrder shipmentOrder;

    private ShipmentOrder insertedShipmentOrder;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ShipmentOrder createEntity(EntityManager em) {
        ShipmentOrder shipmentOrder = new ShipmentOrder()
            .orderCode(DEFAULT_ORDER_CODE)
            .draftCode(DEFAULT_DRAFT_CODE)
            .status(DEFAULT_STATUS)
            .forwardStage(DEFAULT_FORWARD_STAGE)
            .returnStage(DEFAULT_RETURN_STAGE)
            .paymentTerm(DEFAULT_PAYMENT_TERM)
            .goodsType(DEFAULT_GOODS_TYPE)
            .serviceType(DEFAULT_SERVICE_TYPE)
            .senderName(DEFAULT_SENDER_NAME)
            .senderPhone(DEFAULT_SENDER_PHONE)
            .receiverName(DEFAULT_RECEIVER_NAME)
            .receiverPhone(DEFAULT_RECEIVER_PHONE)
            .deliveryAddress(DEFAULT_DELIVERY_ADDRESS)
            .pickupAddress(DEFAULT_PICKUP_ADDRESS)
            .homePickup(DEFAULT_HOME_PICKUP)
            .homeDelivery(DEFAULT_HOME_DELIVERY)
            .qrDropOff(DEFAULT_QR_DROP_OFF)
            .pickupStaffUsername(DEFAULT_PICKUP_STAFF_USERNAME)
            .pickingAt(DEFAULT_PICKING_AT)
            .pickedUpAt(DEFAULT_PICKED_UP_AT)
            .receiverActualName(DEFAULT_RECEIVER_ACTUAL_NAME)
            .receiverActualPhone(DEFAULT_RECEIVER_ACTUAL_PHONE)
            .weightKg(DEFAULT_WEIGHT_KG)
            .quantity(DEFAULT_QUANTITY)
            .dimensionsText(DEFAULT_DIMENSIONS_TEXT)
            .fareAmount(DEFAULT_FARE_AMOUNT)
            .pickupFeeAmount(DEFAULT_PICKUP_FEE_AMOUNT)
            .deliveryFeeAmount(DEFAULT_DELIVERY_FEE_AMOUNT)
            .partnerFeeAmount(DEFAULT_PARTNER_FEE_AMOUNT)
            .paidAmount(DEFAULT_PAID_AMOUNT)
            .shelfNumber(DEFAULT_SHELF_NUMBER)
            .note(DEFAULT_NOTE)
            .cancelReason(DEFAULT_CANCEL_REASON)
            .labelPrintedAt(DEFAULT_LABEL_PRINTED_AT)
            .labelReprintCount(DEFAULT_LABEL_REPRINT_COUNT)
            .failCount(DEFAULT_FAIL_COUNT)
            .partnerCode(DEFAULT_PARTNER_CODE)
            .paymentPercent(DEFAULT_PAYMENT_PERCENT)
            .publicTrackingAllowed(DEFAULT_PUBLIC_TRACKING_ALLOWED);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        shipmentOrder.setFromOffice(office);
        // Add required entity
        shipmentOrder.setToOffice(office);
        return shipmentOrder;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ShipmentOrder createUpdatedEntity(EntityManager em) {
        ShipmentOrder updatedShipmentOrder = new ShipmentOrder()
            .orderCode(UPDATED_ORDER_CODE)
            .draftCode(UPDATED_DRAFT_CODE)
            .status(UPDATED_STATUS)
            .forwardStage(UPDATED_FORWARD_STAGE)
            .returnStage(UPDATED_RETURN_STAGE)
            .paymentTerm(UPDATED_PAYMENT_TERM)
            .goodsType(UPDATED_GOODS_TYPE)
            .serviceType(UPDATED_SERVICE_TYPE)
            .senderName(UPDATED_SENDER_NAME)
            .senderPhone(UPDATED_SENDER_PHONE)
            .receiverName(UPDATED_RECEIVER_NAME)
            .receiverPhone(UPDATED_RECEIVER_PHONE)
            .deliveryAddress(UPDATED_DELIVERY_ADDRESS)
            .pickupAddress(UPDATED_PICKUP_ADDRESS)
            .homePickup(UPDATED_HOME_PICKUP)
            .homeDelivery(UPDATED_HOME_DELIVERY)
            .qrDropOff(UPDATED_QR_DROP_OFF)
            .pickupStaffUsername(UPDATED_PICKUP_STAFF_USERNAME)
            .pickingAt(UPDATED_PICKING_AT)
            .pickedUpAt(UPDATED_PICKED_UP_AT)
            .receiverActualName(UPDATED_RECEIVER_ACTUAL_NAME)
            .receiverActualPhone(UPDATED_RECEIVER_ACTUAL_PHONE)
            .weightKg(UPDATED_WEIGHT_KG)
            .quantity(UPDATED_QUANTITY)
            .dimensionsText(UPDATED_DIMENSIONS_TEXT)
            .fareAmount(UPDATED_FARE_AMOUNT)
            .pickupFeeAmount(UPDATED_PICKUP_FEE_AMOUNT)
            .deliveryFeeAmount(UPDATED_DELIVERY_FEE_AMOUNT)
            .partnerFeeAmount(UPDATED_PARTNER_FEE_AMOUNT)
            .paidAmount(UPDATED_PAID_AMOUNT)
            .shelfNumber(UPDATED_SHELF_NUMBER)
            .note(UPDATED_NOTE)
            .cancelReason(UPDATED_CANCEL_REASON)
            .labelPrintedAt(UPDATED_LABEL_PRINTED_AT)
            .labelReprintCount(UPDATED_LABEL_REPRINT_COUNT)
            .failCount(UPDATED_FAIL_COUNT)
            .partnerCode(UPDATED_PARTNER_CODE)
            .paymentPercent(UPDATED_PAYMENT_PERCENT)
            .publicTrackingAllowed(UPDATED_PUBLIC_TRACKING_ALLOWED);
        // Add required entity
        Office office;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            office = OfficeResourceIT.createUpdatedEntity();
            em.persist(office);
            em.flush();
        } else {
            office = TestUtil.findAll(em, Office.class).get(0);
        }
        updatedShipmentOrder.setFromOffice(office);
        // Add required entity
        updatedShipmentOrder.setToOffice(office);
        return updatedShipmentOrder;
    }

    @BeforeEach
    public void initTest() {
        shipmentOrder = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedShipmentOrder != null) {
            shipmentOrderRepository.delete(insertedShipmentOrder);
            insertedShipmentOrder = null;
        }
    }

    @Test
    @Transactional
    void createShipmentOrder() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ShipmentOrder
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);
        var returnedShipmentOrderDTO = om.readValue(
            restShipmentOrderMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ShipmentOrderDTO.class
        );

        // Validate the ShipmentOrder in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedShipmentOrder = shipmentOrderMapper.toEntity(returnedShipmentOrderDTO);
        assertShipmentOrderUpdatableFieldsEquals(returnedShipmentOrder, getPersistedShipmentOrder(returnedShipmentOrder));

        insertedShipmentOrder = returnedShipmentOrder;
    }

    @Test
    @Transactional
    void createShipmentOrderWithExistingId() throws Exception {
        // Create the ShipmentOrder with an existing ID
        shipmentOrder.setId(1L);
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ShipmentOrder in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkOrderCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setOrderCode(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setStatus(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPaymentTermIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setPaymentTerm(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkGoodsTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setGoodsType(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkServiceTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setServiceType(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSenderPhoneIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setSenderPhone(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReceiverNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setReceiverName(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReceiverPhoneIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setReceiverPhone(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkHomePickupIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setHomePickup(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkHomeDeliveryIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setHomeDelivery(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkQrDropOffIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setQrDropOff(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkQuantityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setQuantity(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFareAmountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setFareAmount(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPaidAmountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setPaidAmount(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFailCountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setFailCount(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPublicTrackingAllowedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        shipmentOrder.setPublicTrackingAllowed(null);

        // Create the ShipmentOrder, which fails.
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        restShipmentOrderMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllShipmentOrders() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList
        restShipmentOrderMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shipmentOrder.getId().intValue())))
            .andExpect(jsonPath("$.[*].orderCode").value(hasItem(DEFAULT_ORDER_CODE)))
            .andExpect(jsonPath("$.[*].draftCode").value(hasItem(DEFAULT_DRAFT_CODE)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].forwardStage").value(hasItem(DEFAULT_FORWARD_STAGE.toString())))
            .andExpect(jsonPath("$.[*].returnStage").value(hasItem(DEFAULT_RETURN_STAGE.toString())))
            .andExpect(jsonPath("$.[*].paymentTerm").value(hasItem(DEFAULT_PAYMENT_TERM.toString())))
            .andExpect(jsonPath("$.[*].goodsType").value(hasItem(DEFAULT_GOODS_TYPE.toString())))
            .andExpect(jsonPath("$.[*].serviceType").value(hasItem(DEFAULT_SERVICE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].senderName").value(hasItem(DEFAULT_SENDER_NAME)))
            .andExpect(jsonPath("$.[*].senderPhone").value(hasItem(DEFAULT_SENDER_PHONE)))
            .andExpect(jsonPath("$.[*].receiverName").value(hasItem(DEFAULT_RECEIVER_NAME)))
            .andExpect(jsonPath("$.[*].receiverPhone").value(hasItem(DEFAULT_RECEIVER_PHONE)))
            .andExpect(jsonPath("$.[*].deliveryAddress").value(hasItem(DEFAULT_DELIVERY_ADDRESS)))
            .andExpect(jsonPath("$.[*].pickupAddress").value(hasItem(DEFAULT_PICKUP_ADDRESS)))
            .andExpect(jsonPath("$.[*].homePickup").value(hasItem(DEFAULT_HOME_PICKUP)))
            .andExpect(jsonPath("$.[*].homeDelivery").value(hasItem(DEFAULT_HOME_DELIVERY)))
            .andExpect(jsonPath("$.[*].qrDropOff").value(hasItem(DEFAULT_QR_DROP_OFF)))
            .andExpect(jsonPath("$.[*].pickupStaffUsername").value(hasItem(DEFAULT_PICKUP_STAFF_USERNAME)))
            .andExpect(jsonPath("$.[*].pickingAt").value(hasItem(DEFAULT_PICKING_AT.toString())))
            .andExpect(jsonPath("$.[*].pickedUpAt").value(hasItem(DEFAULT_PICKED_UP_AT.toString())))
            .andExpect(jsonPath("$.[*].receiverActualName").value(hasItem(DEFAULT_RECEIVER_ACTUAL_NAME)))
            .andExpect(jsonPath("$.[*].receiverActualPhone").value(hasItem(DEFAULT_RECEIVER_ACTUAL_PHONE)))
            .andExpect(jsonPath("$.[*].weightKg").value(hasItem(sameNumber(DEFAULT_WEIGHT_KG))))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].dimensionsText").value(hasItem(DEFAULT_DIMENSIONS_TEXT)))
            .andExpect(jsonPath("$.[*].fareAmount").value(hasItem(sameNumber(DEFAULT_FARE_AMOUNT))))
            .andExpect(jsonPath("$.[*].pickupFeeAmount").value(hasItem(sameNumber(DEFAULT_PICKUP_FEE_AMOUNT))))
            .andExpect(jsonPath("$.[*].deliveryFeeAmount").value(hasItem(sameNumber(DEFAULT_DELIVERY_FEE_AMOUNT))))
            .andExpect(jsonPath("$.[*].partnerFeeAmount").value(hasItem(sameNumber(DEFAULT_PARTNER_FEE_AMOUNT))))
            .andExpect(jsonPath("$.[*].paidAmount").value(hasItem(sameNumber(DEFAULT_PAID_AMOUNT))))
            .andExpect(jsonPath("$.[*].shelfNumber").value(hasItem(DEFAULT_SHELF_NUMBER)))
            .andExpect(jsonPath("$.[*].note").value(hasItem(DEFAULT_NOTE)))
            .andExpect(jsonPath("$.[*].cancelReason").value(hasItem(DEFAULT_CANCEL_REASON)))
            .andExpect(jsonPath("$.[*].labelPrintedAt").value(hasItem(DEFAULT_LABEL_PRINTED_AT.toString())))
            .andExpect(jsonPath("$.[*].labelReprintCount").value(hasItem(DEFAULT_LABEL_REPRINT_COUNT)))
            .andExpect(jsonPath("$.[*].failCount").value(hasItem(DEFAULT_FAIL_COUNT)))
            .andExpect(jsonPath("$.[*].partnerCode").value(hasItem(DEFAULT_PARTNER_CODE)))
            .andExpect(jsonPath("$.[*].paymentPercent").value(hasItem(sameNumber(DEFAULT_PAYMENT_PERCENT))))
            .andExpect(jsonPath("$.[*].publicTrackingAllowed").value(hasItem(DEFAULT_PUBLIC_TRACKING_ALLOWED)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllShipmentOrdersWithEagerRelationshipsIsEnabled() throws Exception {
        when(shipmentOrderServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restShipmentOrderMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(shipmentOrderServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllShipmentOrdersWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(shipmentOrderServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restShipmentOrderMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(shipmentOrderRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getShipmentOrder() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get the shipmentOrder
        restShipmentOrderMockMvc
            .perform(get(ENTITY_API_URL_ID, shipmentOrder.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(shipmentOrder.getId().intValue()))
            .andExpect(jsonPath("$.orderCode").value(DEFAULT_ORDER_CODE))
            .andExpect(jsonPath("$.draftCode").value(DEFAULT_DRAFT_CODE))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.forwardStage").value(DEFAULT_FORWARD_STAGE.toString()))
            .andExpect(jsonPath("$.returnStage").value(DEFAULT_RETURN_STAGE.toString()))
            .andExpect(jsonPath("$.paymentTerm").value(DEFAULT_PAYMENT_TERM.toString()))
            .andExpect(jsonPath("$.goodsType").value(DEFAULT_GOODS_TYPE.toString()))
            .andExpect(jsonPath("$.serviceType").value(DEFAULT_SERVICE_TYPE.toString()))
            .andExpect(jsonPath("$.senderName").value(DEFAULT_SENDER_NAME))
            .andExpect(jsonPath("$.senderPhone").value(DEFAULT_SENDER_PHONE))
            .andExpect(jsonPath("$.receiverName").value(DEFAULT_RECEIVER_NAME))
            .andExpect(jsonPath("$.receiverPhone").value(DEFAULT_RECEIVER_PHONE))
            .andExpect(jsonPath("$.deliveryAddress").value(DEFAULT_DELIVERY_ADDRESS))
            .andExpect(jsonPath("$.pickupAddress").value(DEFAULT_PICKUP_ADDRESS))
            .andExpect(jsonPath("$.homePickup").value(DEFAULT_HOME_PICKUP))
            .andExpect(jsonPath("$.homeDelivery").value(DEFAULT_HOME_DELIVERY))
            .andExpect(jsonPath("$.qrDropOff").value(DEFAULT_QR_DROP_OFF))
            .andExpect(jsonPath("$.pickupStaffUsername").value(DEFAULT_PICKUP_STAFF_USERNAME))
            .andExpect(jsonPath("$.pickingAt").value(DEFAULT_PICKING_AT.toString()))
            .andExpect(jsonPath("$.pickedUpAt").value(DEFAULT_PICKED_UP_AT.toString()))
            .andExpect(jsonPath("$.receiverActualName").value(DEFAULT_RECEIVER_ACTUAL_NAME))
            .andExpect(jsonPath("$.receiverActualPhone").value(DEFAULT_RECEIVER_ACTUAL_PHONE))
            .andExpect(jsonPath("$.weightKg").value(sameNumber(DEFAULT_WEIGHT_KG)))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY))
            .andExpect(jsonPath("$.dimensionsText").value(DEFAULT_DIMENSIONS_TEXT))
            .andExpect(jsonPath("$.fareAmount").value(sameNumber(DEFAULT_FARE_AMOUNT)))
            .andExpect(jsonPath("$.pickupFeeAmount").value(sameNumber(DEFAULT_PICKUP_FEE_AMOUNT)))
            .andExpect(jsonPath("$.deliveryFeeAmount").value(sameNumber(DEFAULT_DELIVERY_FEE_AMOUNT)))
            .andExpect(jsonPath("$.partnerFeeAmount").value(sameNumber(DEFAULT_PARTNER_FEE_AMOUNT)))
            .andExpect(jsonPath("$.paidAmount").value(sameNumber(DEFAULT_PAID_AMOUNT)))
            .andExpect(jsonPath("$.shelfNumber").value(DEFAULT_SHELF_NUMBER))
            .andExpect(jsonPath("$.note").value(DEFAULT_NOTE))
            .andExpect(jsonPath("$.cancelReason").value(DEFAULT_CANCEL_REASON))
            .andExpect(jsonPath("$.labelPrintedAt").value(DEFAULT_LABEL_PRINTED_AT.toString()))
            .andExpect(jsonPath("$.labelReprintCount").value(DEFAULT_LABEL_REPRINT_COUNT))
            .andExpect(jsonPath("$.failCount").value(DEFAULT_FAIL_COUNT))
            .andExpect(jsonPath("$.partnerCode").value(DEFAULT_PARTNER_CODE))
            .andExpect(jsonPath("$.paymentPercent").value(sameNumber(DEFAULT_PAYMENT_PERCENT)))
            .andExpect(jsonPath("$.publicTrackingAllowed").value(DEFAULT_PUBLIC_TRACKING_ALLOWED));
    }

    @Test
    @Transactional
    void getShipmentOrdersByIdFiltering() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        Long id = shipmentOrder.getId();

        defaultShipmentOrderFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultShipmentOrderFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultShipmentOrderFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByOrderCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where orderCode equals to
        defaultShipmentOrderFiltering("orderCode.equals=" + DEFAULT_ORDER_CODE, "orderCode.equals=" + UPDATED_ORDER_CODE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByOrderCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where orderCode in
        defaultShipmentOrderFiltering(
            "orderCode.in=" + DEFAULT_ORDER_CODE + "," + UPDATED_ORDER_CODE,
            "orderCode.in=" + UPDATED_ORDER_CODE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByOrderCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where orderCode is not null
        defaultShipmentOrderFiltering("orderCode.specified=true", "orderCode.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByOrderCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where orderCode contains
        defaultShipmentOrderFiltering("orderCode.contains=" + DEFAULT_ORDER_CODE, "orderCode.contains=" + UPDATED_ORDER_CODE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByOrderCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where orderCode does not contain
        defaultShipmentOrderFiltering("orderCode.doesNotContain=" + UPDATED_ORDER_CODE, "orderCode.doesNotContain=" + DEFAULT_ORDER_CODE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDraftCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where draftCode equals to
        defaultShipmentOrderFiltering("draftCode.equals=" + DEFAULT_DRAFT_CODE, "draftCode.equals=" + UPDATED_DRAFT_CODE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDraftCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where draftCode in
        defaultShipmentOrderFiltering(
            "draftCode.in=" + DEFAULT_DRAFT_CODE + "," + UPDATED_DRAFT_CODE,
            "draftCode.in=" + UPDATED_DRAFT_CODE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDraftCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where draftCode is not null
        defaultShipmentOrderFiltering("draftCode.specified=true", "draftCode.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDraftCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where draftCode contains
        defaultShipmentOrderFiltering("draftCode.contains=" + DEFAULT_DRAFT_CODE, "draftCode.contains=" + UPDATED_DRAFT_CODE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDraftCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where draftCode does not contain
        defaultShipmentOrderFiltering("draftCode.doesNotContain=" + UPDATED_DRAFT_CODE, "draftCode.doesNotContain=" + DEFAULT_DRAFT_CODE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where status equals to
        defaultShipmentOrderFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where status in
        defaultShipmentOrderFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where status is not null
        defaultShipmentOrderFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByForwardStageIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where forwardStage equals to
        defaultShipmentOrderFiltering("forwardStage.equals=" + DEFAULT_FORWARD_STAGE, "forwardStage.equals=" + UPDATED_FORWARD_STAGE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByForwardStageIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where forwardStage in
        defaultShipmentOrderFiltering(
            "forwardStage.in=" + DEFAULT_FORWARD_STAGE + "," + UPDATED_FORWARD_STAGE,
            "forwardStage.in=" + UPDATED_FORWARD_STAGE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByForwardStageIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where forwardStage is not null
        defaultShipmentOrderFiltering("forwardStage.specified=true", "forwardStage.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReturnStageIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where returnStage equals to
        defaultShipmentOrderFiltering("returnStage.equals=" + DEFAULT_RETURN_STAGE, "returnStage.equals=" + UPDATED_RETURN_STAGE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReturnStageIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where returnStage in
        defaultShipmentOrderFiltering(
            "returnStage.in=" + DEFAULT_RETURN_STAGE + "," + UPDATED_RETURN_STAGE,
            "returnStage.in=" + UPDATED_RETURN_STAGE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReturnStageIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where returnStage is not null
        defaultShipmentOrderFiltering("returnStage.specified=true", "returnStage.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentTermIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentTerm equals to
        defaultShipmentOrderFiltering("paymentTerm.equals=" + DEFAULT_PAYMENT_TERM, "paymentTerm.equals=" + UPDATED_PAYMENT_TERM);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentTermIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentTerm in
        defaultShipmentOrderFiltering(
            "paymentTerm.in=" + DEFAULT_PAYMENT_TERM + "," + UPDATED_PAYMENT_TERM,
            "paymentTerm.in=" + UPDATED_PAYMENT_TERM
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentTermIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentTerm is not null
        defaultShipmentOrderFiltering("paymentTerm.specified=true", "paymentTerm.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByGoodsTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where goodsType equals to
        defaultShipmentOrderFiltering("goodsType.equals=" + DEFAULT_GOODS_TYPE, "goodsType.equals=" + UPDATED_GOODS_TYPE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByGoodsTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where goodsType in
        defaultShipmentOrderFiltering(
            "goodsType.in=" + DEFAULT_GOODS_TYPE + "," + UPDATED_GOODS_TYPE,
            "goodsType.in=" + UPDATED_GOODS_TYPE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByGoodsTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where goodsType is not null
        defaultShipmentOrderFiltering("goodsType.specified=true", "goodsType.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByServiceTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where serviceType equals to
        defaultShipmentOrderFiltering("serviceType.equals=" + DEFAULT_SERVICE_TYPE, "serviceType.equals=" + UPDATED_SERVICE_TYPE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByServiceTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where serviceType in
        defaultShipmentOrderFiltering(
            "serviceType.in=" + DEFAULT_SERVICE_TYPE + "," + UPDATED_SERVICE_TYPE,
            "serviceType.in=" + UPDATED_SERVICE_TYPE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByServiceTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where serviceType is not null
        defaultShipmentOrderFiltering("serviceType.specified=true", "serviceType.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderName equals to
        defaultShipmentOrderFiltering("senderName.equals=" + DEFAULT_SENDER_NAME, "senderName.equals=" + UPDATED_SENDER_NAME);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderName in
        defaultShipmentOrderFiltering(
            "senderName.in=" + DEFAULT_SENDER_NAME + "," + UPDATED_SENDER_NAME,
            "senderName.in=" + UPDATED_SENDER_NAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderName is not null
        defaultShipmentOrderFiltering("senderName.specified=true", "senderName.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderNameContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderName contains
        defaultShipmentOrderFiltering("senderName.contains=" + DEFAULT_SENDER_NAME, "senderName.contains=" + UPDATED_SENDER_NAME);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderName does not contain
        defaultShipmentOrderFiltering(
            "senderName.doesNotContain=" + UPDATED_SENDER_NAME,
            "senderName.doesNotContain=" + DEFAULT_SENDER_NAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderPhoneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderPhone equals to
        defaultShipmentOrderFiltering("senderPhone.equals=" + DEFAULT_SENDER_PHONE, "senderPhone.equals=" + UPDATED_SENDER_PHONE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderPhoneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderPhone in
        defaultShipmentOrderFiltering(
            "senderPhone.in=" + DEFAULT_SENDER_PHONE + "," + UPDATED_SENDER_PHONE,
            "senderPhone.in=" + UPDATED_SENDER_PHONE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderPhoneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderPhone is not null
        defaultShipmentOrderFiltering("senderPhone.specified=true", "senderPhone.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderPhoneContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderPhone contains
        defaultShipmentOrderFiltering("senderPhone.contains=" + DEFAULT_SENDER_PHONE, "senderPhone.contains=" + UPDATED_SENDER_PHONE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderPhoneNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where senderPhone does not contain
        defaultShipmentOrderFiltering(
            "senderPhone.doesNotContain=" + UPDATED_SENDER_PHONE,
            "senderPhone.doesNotContain=" + DEFAULT_SENDER_PHONE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverName equals to
        defaultShipmentOrderFiltering("receiverName.equals=" + DEFAULT_RECEIVER_NAME, "receiverName.equals=" + UPDATED_RECEIVER_NAME);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverName in
        defaultShipmentOrderFiltering(
            "receiverName.in=" + DEFAULT_RECEIVER_NAME + "," + UPDATED_RECEIVER_NAME,
            "receiverName.in=" + UPDATED_RECEIVER_NAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverName is not null
        defaultShipmentOrderFiltering("receiverName.specified=true", "receiverName.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverNameContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverName contains
        defaultShipmentOrderFiltering("receiverName.contains=" + DEFAULT_RECEIVER_NAME, "receiverName.contains=" + UPDATED_RECEIVER_NAME);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverName does not contain
        defaultShipmentOrderFiltering(
            "receiverName.doesNotContain=" + UPDATED_RECEIVER_NAME,
            "receiverName.doesNotContain=" + DEFAULT_RECEIVER_NAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverPhoneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverPhone equals to
        defaultShipmentOrderFiltering("receiverPhone.equals=" + DEFAULT_RECEIVER_PHONE, "receiverPhone.equals=" + UPDATED_RECEIVER_PHONE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverPhoneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverPhone in
        defaultShipmentOrderFiltering(
            "receiverPhone.in=" + DEFAULT_RECEIVER_PHONE + "," + UPDATED_RECEIVER_PHONE,
            "receiverPhone.in=" + UPDATED_RECEIVER_PHONE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverPhoneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverPhone is not null
        defaultShipmentOrderFiltering("receiverPhone.specified=true", "receiverPhone.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverPhoneContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverPhone contains
        defaultShipmentOrderFiltering(
            "receiverPhone.contains=" + DEFAULT_RECEIVER_PHONE,
            "receiverPhone.contains=" + UPDATED_RECEIVER_PHONE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverPhoneNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverPhone does not contain
        defaultShipmentOrderFiltering(
            "receiverPhone.doesNotContain=" + UPDATED_RECEIVER_PHONE,
            "receiverPhone.doesNotContain=" + DEFAULT_RECEIVER_PHONE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryAddressIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryAddress equals to
        defaultShipmentOrderFiltering(
            "deliveryAddress.equals=" + DEFAULT_DELIVERY_ADDRESS,
            "deliveryAddress.equals=" + UPDATED_DELIVERY_ADDRESS
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryAddressIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryAddress in
        defaultShipmentOrderFiltering(
            "deliveryAddress.in=" + DEFAULT_DELIVERY_ADDRESS + "," + UPDATED_DELIVERY_ADDRESS,
            "deliveryAddress.in=" + UPDATED_DELIVERY_ADDRESS
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryAddressIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryAddress is not null
        defaultShipmentOrderFiltering("deliveryAddress.specified=true", "deliveryAddress.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryAddressContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryAddress contains
        defaultShipmentOrderFiltering(
            "deliveryAddress.contains=" + DEFAULT_DELIVERY_ADDRESS,
            "deliveryAddress.contains=" + UPDATED_DELIVERY_ADDRESS
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryAddressNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryAddress does not contain
        defaultShipmentOrderFiltering(
            "deliveryAddress.doesNotContain=" + UPDATED_DELIVERY_ADDRESS,
            "deliveryAddress.doesNotContain=" + DEFAULT_DELIVERY_ADDRESS
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupAddressIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupAddress equals to
        defaultShipmentOrderFiltering("pickupAddress.equals=" + DEFAULT_PICKUP_ADDRESS, "pickupAddress.equals=" + UPDATED_PICKUP_ADDRESS);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupAddressIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupAddress in
        defaultShipmentOrderFiltering(
            "pickupAddress.in=" + DEFAULT_PICKUP_ADDRESS + "," + UPDATED_PICKUP_ADDRESS,
            "pickupAddress.in=" + UPDATED_PICKUP_ADDRESS
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupAddressIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupAddress is not null
        defaultShipmentOrderFiltering("pickupAddress.specified=true", "pickupAddress.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupAddressContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupAddress contains
        defaultShipmentOrderFiltering(
            "pickupAddress.contains=" + DEFAULT_PICKUP_ADDRESS,
            "pickupAddress.contains=" + UPDATED_PICKUP_ADDRESS
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupAddressNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupAddress does not contain
        defaultShipmentOrderFiltering(
            "pickupAddress.doesNotContain=" + UPDATED_PICKUP_ADDRESS,
            "pickupAddress.doesNotContain=" + DEFAULT_PICKUP_ADDRESS
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByHomePickupIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where homePickup equals to
        defaultShipmentOrderFiltering("homePickup.equals=" + DEFAULT_HOME_PICKUP, "homePickup.equals=" + UPDATED_HOME_PICKUP);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByHomePickupIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where homePickup in
        defaultShipmentOrderFiltering(
            "homePickup.in=" + DEFAULT_HOME_PICKUP + "," + UPDATED_HOME_PICKUP,
            "homePickup.in=" + UPDATED_HOME_PICKUP
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByHomePickupIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where homePickup is not null
        defaultShipmentOrderFiltering("homePickup.specified=true", "homePickup.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByHomeDeliveryIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where homeDelivery equals to
        defaultShipmentOrderFiltering("homeDelivery.equals=" + DEFAULT_HOME_DELIVERY, "homeDelivery.equals=" + UPDATED_HOME_DELIVERY);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByHomeDeliveryIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where homeDelivery in
        defaultShipmentOrderFiltering(
            "homeDelivery.in=" + DEFAULT_HOME_DELIVERY + "," + UPDATED_HOME_DELIVERY,
            "homeDelivery.in=" + UPDATED_HOME_DELIVERY
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByHomeDeliveryIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where homeDelivery is not null
        defaultShipmentOrderFiltering("homeDelivery.specified=true", "homeDelivery.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQrDropOffIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where qrDropOff equals to
        defaultShipmentOrderFiltering("qrDropOff.equals=" + DEFAULT_QR_DROP_OFF, "qrDropOff.equals=" + UPDATED_QR_DROP_OFF);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQrDropOffIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where qrDropOff in
        defaultShipmentOrderFiltering(
            "qrDropOff.in=" + DEFAULT_QR_DROP_OFF + "," + UPDATED_QR_DROP_OFF,
            "qrDropOff.in=" + UPDATED_QR_DROP_OFF
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQrDropOffIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where qrDropOff is not null
        defaultShipmentOrderFiltering("qrDropOff.specified=true", "qrDropOff.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupStaffUsernameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupStaffUsername equals to
        defaultShipmentOrderFiltering(
            "pickupStaffUsername.equals=" + DEFAULT_PICKUP_STAFF_USERNAME,
            "pickupStaffUsername.equals=" + UPDATED_PICKUP_STAFF_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupStaffUsernameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupStaffUsername in
        defaultShipmentOrderFiltering(
            "pickupStaffUsername.in=" + DEFAULT_PICKUP_STAFF_USERNAME + "," + UPDATED_PICKUP_STAFF_USERNAME,
            "pickupStaffUsername.in=" + UPDATED_PICKUP_STAFF_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupStaffUsernameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupStaffUsername is not null
        defaultShipmentOrderFiltering("pickupStaffUsername.specified=true", "pickupStaffUsername.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupStaffUsernameContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupStaffUsername contains
        defaultShipmentOrderFiltering(
            "pickupStaffUsername.contains=" + DEFAULT_PICKUP_STAFF_USERNAME,
            "pickupStaffUsername.contains=" + UPDATED_PICKUP_STAFF_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupStaffUsernameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupStaffUsername does not contain
        defaultShipmentOrderFiltering(
            "pickupStaffUsername.doesNotContain=" + UPDATED_PICKUP_STAFF_USERNAME,
            "pickupStaffUsername.doesNotContain=" + DEFAULT_PICKUP_STAFF_USERNAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickingAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickingAt equals to
        defaultShipmentOrderFiltering("pickingAt.equals=" + DEFAULT_PICKING_AT, "pickingAt.equals=" + UPDATED_PICKING_AT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickingAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickingAt in
        defaultShipmentOrderFiltering(
            "pickingAt.in=" + DEFAULT_PICKING_AT + "," + UPDATED_PICKING_AT,
            "pickingAt.in=" + UPDATED_PICKING_AT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickingAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickingAt is not null
        defaultShipmentOrderFiltering("pickingAt.specified=true", "pickingAt.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickedUpAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickedUpAt equals to
        defaultShipmentOrderFiltering("pickedUpAt.equals=" + DEFAULT_PICKED_UP_AT, "pickedUpAt.equals=" + UPDATED_PICKED_UP_AT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickedUpAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickedUpAt in
        defaultShipmentOrderFiltering(
            "pickedUpAt.in=" + DEFAULT_PICKED_UP_AT + "," + UPDATED_PICKED_UP_AT,
            "pickedUpAt.in=" + UPDATED_PICKED_UP_AT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickedUpAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickedUpAt is not null
        defaultShipmentOrderFiltering("pickedUpAt.specified=true", "pickedUpAt.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualName equals to
        defaultShipmentOrderFiltering(
            "receiverActualName.equals=" + DEFAULT_RECEIVER_ACTUAL_NAME,
            "receiverActualName.equals=" + UPDATED_RECEIVER_ACTUAL_NAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualName in
        defaultShipmentOrderFiltering(
            "receiverActualName.in=" + DEFAULT_RECEIVER_ACTUAL_NAME + "," + UPDATED_RECEIVER_ACTUAL_NAME,
            "receiverActualName.in=" + UPDATED_RECEIVER_ACTUAL_NAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualName is not null
        defaultShipmentOrderFiltering("receiverActualName.specified=true", "receiverActualName.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualNameContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualName contains
        defaultShipmentOrderFiltering(
            "receiverActualName.contains=" + DEFAULT_RECEIVER_ACTUAL_NAME,
            "receiverActualName.contains=" + UPDATED_RECEIVER_ACTUAL_NAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualName does not contain
        defaultShipmentOrderFiltering(
            "receiverActualName.doesNotContain=" + UPDATED_RECEIVER_ACTUAL_NAME,
            "receiverActualName.doesNotContain=" + DEFAULT_RECEIVER_ACTUAL_NAME
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualPhoneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualPhone equals to
        defaultShipmentOrderFiltering(
            "receiverActualPhone.equals=" + DEFAULT_RECEIVER_ACTUAL_PHONE,
            "receiverActualPhone.equals=" + UPDATED_RECEIVER_ACTUAL_PHONE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualPhoneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualPhone in
        defaultShipmentOrderFiltering(
            "receiverActualPhone.in=" + DEFAULT_RECEIVER_ACTUAL_PHONE + "," + UPDATED_RECEIVER_ACTUAL_PHONE,
            "receiverActualPhone.in=" + UPDATED_RECEIVER_ACTUAL_PHONE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualPhoneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualPhone is not null
        defaultShipmentOrderFiltering("receiverActualPhone.specified=true", "receiverActualPhone.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualPhoneContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualPhone contains
        defaultShipmentOrderFiltering(
            "receiverActualPhone.contains=" + DEFAULT_RECEIVER_ACTUAL_PHONE,
            "receiverActualPhone.contains=" + UPDATED_RECEIVER_ACTUAL_PHONE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReceiverActualPhoneNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where receiverActualPhone does not contain
        defaultShipmentOrderFiltering(
            "receiverActualPhone.doesNotContain=" + UPDATED_RECEIVER_ACTUAL_PHONE,
            "receiverActualPhone.doesNotContain=" + DEFAULT_RECEIVER_ACTUAL_PHONE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByWeightKgIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where weightKg equals to
        defaultShipmentOrderFiltering("weightKg.equals=" + DEFAULT_WEIGHT_KG, "weightKg.equals=" + UPDATED_WEIGHT_KG);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByWeightKgIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where weightKg in
        defaultShipmentOrderFiltering("weightKg.in=" + DEFAULT_WEIGHT_KG + "," + UPDATED_WEIGHT_KG, "weightKg.in=" + UPDATED_WEIGHT_KG);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByWeightKgIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where weightKg is not null
        defaultShipmentOrderFiltering("weightKg.specified=true", "weightKg.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByWeightKgIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where weightKg is greater than or equal to
        defaultShipmentOrderFiltering(
            "weightKg.greaterThanOrEqual=" + DEFAULT_WEIGHT_KG,
            "weightKg.greaterThanOrEqual=" + UPDATED_WEIGHT_KG
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByWeightKgIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where weightKg is less than or equal to
        defaultShipmentOrderFiltering("weightKg.lessThanOrEqual=" + DEFAULT_WEIGHT_KG, "weightKg.lessThanOrEqual=" + SMALLER_WEIGHT_KG);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByWeightKgIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where weightKg is less than
        defaultShipmentOrderFiltering("weightKg.lessThan=" + UPDATED_WEIGHT_KG, "weightKg.lessThan=" + DEFAULT_WEIGHT_KG);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByWeightKgIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where weightKg is greater than
        defaultShipmentOrderFiltering("weightKg.greaterThan=" + SMALLER_WEIGHT_KG, "weightKg.greaterThan=" + DEFAULT_WEIGHT_KG);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQuantityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where quantity equals to
        defaultShipmentOrderFiltering("quantity.equals=" + DEFAULT_QUANTITY, "quantity.equals=" + UPDATED_QUANTITY);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQuantityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where quantity in
        defaultShipmentOrderFiltering("quantity.in=" + DEFAULT_QUANTITY + "," + UPDATED_QUANTITY, "quantity.in=" + UPDATED_QUANTITY);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQuantityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where quantity is not null
        defaultShipmentOrderFiltering("quantity.specified=true", "quantity.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQuantityIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where quantity is greater than or equal to
        defaultShipmentOrderFiltering("quantity.greaterThanOrEqual=" + DEFAULT_QUANTITY, "quantity.greaterThanOrEqual=" + UPDATED_QUANTITY);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQuantityIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where quantity is less than or equal to
        defaultShipmentOrderFiltering("quantity.lessThanOrEqual=" + DEFAULT_QUANTITY, "quantity.lessThanOrEqual=" + SMALLER_QUANTITY);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQuantityIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where quantity is less than
        defaultShipmentOrderFiltering("quantity.lessThan=" + UPDATED_QUANTITY, "quantity.lessThan=" + DEFAULT_QUANTITY);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByQuantityIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where quantity is greater than
        defaultShipmentOrderFiltering("quantity.greaterThan=" + SMALLER_QUANTITY, "quantity.greaterThan=" + DEFAULT_QUANTITY);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDimensionsTextIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where dimensionsText equals to
        defaultShipmentOrderFiltering(
            "dimensionsText.equals=" + DEFAULT_DIMENSIONS_TEXT,
            "dimensionsText.equals=" + UPDATED_DIMENSIONS_TEXT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDimensionsTextIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where dimensionsText in
        defaultShipmentOrderFiltering(
            "dimensionsText.in=" + DEFAULT_DIMENSIONS_TEXT + "," + UPDATED_DIMENSIONS_TEXT,
            "dimensionsText.in=" + UPDATED_DIMENSIONS_TEXT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDimensionsTextIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where dimensionsText is not null
        defaultShipmentOrderFiltering("dimensionsText.specified=true", "dimensionsText.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDimensionsTextContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where dimensionsText contains
        defaultShipmentOrderFiltering(
            "dimensionsText.contains=" + DEFAULT_DIMENSIONS_TEXT,
            "dimensionsText.contains=" + UPDATED_DIMENSIONS_TEXT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDimensionsTextNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where dimensionsText does not contain
        defaultShipmentOrderFiltering(
            "dimensionsText.doesNotContain=" + UPDATED_DIMENSIONS_TEXT,
            "dimensionsText.doesNotContain=" + DEFAULT_DIMENSIONS_TEXT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFareAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where fareAmount equals to
        defaultShipmentOrderFiltering("fareAmount.equals=" + DEFAULT_FARE_AMOUNT, "fareAmount.equals=" + UPDATED_FARE_AMOUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFareAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where fareAmount in
        defaultShipmentOrderFiltering(
            "fareAmount.in=" + DEFAULT_FARE_AMOUNT + "," + UPDATED_FARE_AMOUNT,
            "fareAmount.in=" + UPDATED_FARE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFareAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where fareAmount is not null
        defaultShipmentOrderFiltering("fareAmount.specified=true", "fareAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFareAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where fareAmount is greater than or equal to
        defaultShipmentOrderFiltering(
            "fareAmount.greaterThanOrEqual=" + DEFAULT_FARE_AMOUNT,
            "fareAmount.greaterThanOrEqual=" + UPDATED_FARE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFareAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where fareAmount is less than or equal to
        defaultShipmentOrderFiltering(
            "fareAmount.lessThanOrEqual=" + DEFAULT_FARE_AMOUNT,
            "fareAmount.lessThanOrEqual=" + SMALLER_FARE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFareAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where fareAmount is less than
        defaultShipmentOrderFiltering("fareAmount.lessThan=" + UPDATED_FARE_AMOUNT, "fareAmount.lessThan=" + DEFAULT_FARE_AMOUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFareAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where fareAmount is greater than
        defaultShipmentOrderFiltering("fareAmount.greaterThan=" + SMALLER_FARE_AMOUNT, "fareAmount.greaterThan=" + DEFAULT_FARE_AMOUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupFeeAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupFeeAmount equals to
        defaultShipmentOrderFiltering(
            "pickupFeeAmount.equals=" + DEFAULT_PICKUP_FEE_AMOUNT,
            "pickupFeeAmount.equals=" + UPDATED_PICKUP_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupFeeAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupFeeAmount in
        defaultShipmentOrderFiltering(
            "pickupFeeAmount.in=" + DEFAULT_PICKUP_FEE_AMOUNT + "," + UPDATED_PICKUP_FEE_AMOUNT,
            "pickupFeeAmount.in=" + UPDATED_PICKUP_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupFeeAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupFeeAmount is not null
        defaultShipmentOrderFiltering("pickupFeeAmount.specified=true", "pickupFeeAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupFeeAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupFeeAmount is greater than or equal to
        defaultShipmentOrderFiltering(
            "pickupFeeAmount.greaterThanOrEqual=" + DEFAULT_PICKUP_FEE_AMOUNT,
            "pickupFeeAmount.greaterThanOrEqual=" + UPDATED_PICKUP_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupFeeAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupFeeAmount is less than or equal to
        defaultShipmentOrderFiltering(
            "pickupFeeAmount.lessThanOrEqual=" + DEFAULT_PICKUP_FEE_AMOUNT,
            "pickupFeeAmount.lessThanOrEqual=" + SMALLER_PICKUP_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupFeeAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupFeeAmount is less than
        defaultShipmentOrderFiltering(
            "pickupFeeAmount.lessThan=" + UPDATED_PICKUP_FEE_AMOUNT,
            "pickupFeeAmount.lessThan=" + DEFAULT_PICKUP_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPickupFeeAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where pickupFeeAmount is greater than
        defaultShipmentOrderFiltering(
            "pickupFeeAmount.greaterThan=" + SMALLER_PICKUP_FEE_AMOUNT,
            "pickupFeeAmount.greaterThan=" + DEFAULT_PICKUP_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryFeeAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryFeeAmount equals to
        defaultShipmentOrderFiltering(
            "deliveryFeeAmount.equals=" + DEFAULT_DELIVERY_FEE_AMOUNT,
            "deliveryFeeAmount.equals=" + UPDATED_DELIVERY_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryFeeAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryFeeAmount in
        defaultShipmentOrderFiltering(
            "deliveryFeeAmount.in=" + DEFAULT_DELIVERY_FEE_AMOUNT + "," + UPDATED_DELIVERY_FEE_AMOUNT,
            "deliveryFeeAmount.in=" + UPDATED_DELIVERY_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryFeeAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryFeeAmount is not null
        defaultShipmentOrderFiltering("deliveryFeeAmount.specified=true", "deliveryFeeAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryFeeAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryFeeAmount is greater than or equal to
        defaultShipmentOrderFiltering(
            "deliveryFeeAmount.greaterThanOrEqual=" + DEFAULT_DELIVERY_FEE_AMOUNT,
            "deliveryFeeAmount.greaterThanOrEqual=" + UPDATED_DELIVERY_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryFeeAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryFeeAmount is less than or equal to
        defaultShipmentOrderFiltering(
            "deliveryFeeAmount.lessThanOrEqual=" + DEFAULT_DELIVERY_FEE_AMOUNT,
            "deliveryFeeAmount.lessThanOrEqual=" + SMALLER_DELIVERY_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryFeeAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryFeeAmount is less than
        defaultShipmentOrderFiltering(
            "deliveryFeeAmount.lessThan=" + UPDATED_DELIVERY_FEE_AMOUNT,
            "deliveryFeeAmount.lessThan=" + DEFAULT_DELIVERY_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByDeliveryFeeAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where deliveryFeeAmount is greater than
        defaultShipmentOrderFiltering(
            "deliveryFeeAmount.greaterThan=" + SMALLER_DELIVERY_FEE_AMOUNT,
            "deliveryFeeAmount.greaterThan=" + DEFAULT_DELIVERY_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerFeeAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerFeeAmount equals to
        defaultShipmentOrderFiltering(
            "partnerFeeAmount.equals=" + DEFAULT_PARTNER_FEE_AMOUNT,
            "partnerFeeAmount.equals=" + UPDATED_PARTNER_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerFeeAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerFeeAmount in
        defaultShipmentOrderFiltering(
            "partnerFeeAmount.in=" + DEFAULT_PARTNER_FEE_AMOUNT + "," + UPDATED_PARTNER_FEE_AMOUNT,
            "partnerFeeAmount.in=" + UPDATED_PARTNER_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerFeeAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerFeeAmount is not null
        defaultShipmentOrderFiltering("partnerFeeAmount.specified=true", "partnerFeeAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerFeeAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerFeeAmount is greater than or equal to
        defaultShipmentOrderFiltering(
            "partnerFeeAmount.greaterThanOrEqual=" + DEFAULT_PARTNER_FEE_AMOUNT,
            "partnerFeeAmount.greaterThanOrEqual=" + UPDATED_PARTNER_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerFeeAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerFeeAmount is less than or equal to
        defaultShipmentOrderFiltering(
            "partnerFeeAmount.lessThanOrEqual=" + DEFAULT_PARTNER_FEE_AMOUNT,
            "partnerFeeAmount.lessThanOrEqual=" + SMALLER_PARTNER_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerFeeAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerFeeAmount is less than
        defaultShipmentOrderFiltering(
            "partnerFeeAmount.lessThan=" + UPDATED_PARTNER_FEE_AMOUNT,
            "partnerFeeAmount.lessThan=" + DEFAULT_PARTNER_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerFeeAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerFeeAmount is greater than
        defaultShipmentOrderFiltering(
            "partnerFeeAmount.greaterThan=" + SMALLER_PARTNER_FEE_AMOUNT,
            "partnerFeeAmount.greaterThan=" + DEFAULT_PARTNER_FEE_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaidAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paidAmount equals to
        defaultShipmentOrderFiltering("paidAmount.equals=" + DEFAULT_PAID_AMOUNT, "paidAmount.equals=" + UPDATED_PAID_AMOUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaidAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paidAmount in
        defaultShipmentOrderFiltering(
            "paidAmount.in=" + DEFAULT_PAID_AMOUNT + "," + UPDATED_PAID_AMOUNT,
            "paidAmount.in=" + UPDATED_PAID_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaidAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paidAmount is not null
        defaultShipmentOrderFiltering("paidAmount.specified=true", "paidAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaidAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paidAmount is greater than or equal to
        defaultShipmentOrderFiltering(
            "paidAmount.greaterThanOrEqual=" + DEFAULT_PAID_AMOUNT,
            "paidAmount.greaterThanOrEqual=" + UPDATED_PAID_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaidAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paidAmount is less than or equal to
        defaultShipmentOrderFiltering(
            "paidAmount.lessThanOrEqual=" + DEFAULT_PAID_AMOUNT,
            "paidAmount.lessThanOrEqual=" + SMALLER_PAID_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaidAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paidAmount is less than
        defaultShipmentOrderFiltering("paidAmount.lessThan=" + UPDATED_PAID_AMOUNT, "paidAmount.lessThan=" + DEFAULT_PAID_AMOUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaidAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paidAmount is greater than
        defaultShipmentOrderFiltering("paidAmount.greaterThan=" + SMALLER_PAID_AMOUNT, "paidAmount.greaterThan=" + DEFAULT_PAID_AMOUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByShelfNumberIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where shelfNumber equals to
        defaultShipmentOrderFiltering("shelfNumber.equals=" + DEFAULT_SHELF_NUMBER, "shelfNumber.equals=" + UPDATED_SHELF_NUMBER);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByShelfNumberIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where shelfNumber in
        defaultShipmentOrderFiltering(
            "shelfNumber.in=" + DEFAULT_SHELF_NUMBER + "," + UPDATED_SHELF_NUMBER,
            "shelfNumber.in=" + UPDATED_SHELF_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByShelfNumberIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where shelfNumber is not null
        defaultShipmentOrderFiltering("shelfNumber.specified=true", "shelfNumber.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByShelfNumberIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where shelfNumber is greater than or equal to
        defaultShipmentOrderFiltering(
            "shelfNumber.greaterThanOrEqual=" + DEFAULT_SHELF_NUMBER,
            "shelfNumber.greaterThanOrEqual=" + (DEFAULT_SHELF_NUMBER + 1)
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByShelfNumberIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where shelfNumber is less than or equal to
        defaultShipmentOrderFiltering(
            "shelfNumber.lessThanOrEqual=" + DEFAULT_SHELF_NUMBER,
            "shelfNumber.lessThanOrEqual=" + SMALLER_SHELF_NUMBER
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByShelfNumberIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where shelfNumber is less than
        defaultShipmentOrderFiltering("shelfNumber.lessThan=" + (DEFAULT_SHELF_NUMBER + 1), "shelfNumber.lessThan=" + DEFAULT_SHELF_NUMBER);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByShelfNumberIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where shelfNumber is greater than
        defaultShipmentOrderFiltering("shelfNumber.greaterThan=" + SMALLER_SHELF_NUMBER, "shelfNumber.greaterThan=" + DEFAULT_SHELF_NUMBER);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByCancelReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where cancelReason equals to
        defaultShipmentOrderFiltering("cancelReason.equals=" + DEFAULT_CANCEL_REASON, "cancelReason.equals=" + UPDATED_CANCEL_REASON);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByCancelReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where cancelReason in
        defaultShipmentOrderFiltering(
            "cancelReason.in=" + DEFAULT_CANCEL_REASON + "," + UPDATED_CANCEL_REASON,
            "cancelReason.in=" + UPDATED_CANCEL_REASON
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByCancelReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where cancelReason is not null
        defaultShipmentOrderFiltering("cancelReason.specified=true", "cancelReason.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByCancelReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where cancelReason contains
        defaultShipmentOrderFiltering("cancelReason.contains=" + DEFAULT_CANCEL_REASON, "cancelReason.contains=" + UPDATED_CANCEL_REASON);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByCancelReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where cancelReason does not contain
        defaultShipmentOrderFiltering(
            "cancelReason.doesNotContain=" + UPDATED_CANCEL_REASON,
            "cancelReason.doesNotContain=" + DEFAULT_CANCEL_REASON
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelPrintedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelPrintedAt equals to
        defaultShipmentOrderFiltering(
            "labelPrintedAt.equals=" + DEFAULT_LABEL_PRINTED_AT,
            "labelPrintedAt.equals=" + UPDATED_LABEL_PRINTED_AT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelPrintedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelPrintedAt in
        defaultShipmentOrderFiltering(
            "labelPrintedAt.in=" + DEFAULT_LABEL_PRINTED_AT + "," + UPDATED_LABEL_PRINTED_AT,
            "labelPrintedAt.in=" + UPDATED_LABEL_PRINTED_AT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelPrintedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelPrintedAt is not null
        defaultShipmentOrderFiltering("labelPrintedAt.specified=true", "labelPrintedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelReprintCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelReprintCount equals to
        defaultShipmentOrderFiltering(
            "labelReprintCount.equals=" + DEFAULT_LABEL_REPRINT_COUNT,
            "labelReprintCount.equals=" + UPDATED_LABEL_REPRINT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelReprintCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelReprintCount in
        defaultShipmentOrderFiltering(
            "labelReprintCount.in=" + DEFAULT_LABEL_REPRINT_COUNT + "," + UPDATED_LABEL_REPRINT_COUNT,
            "labelReprintCount.in=" + UPDATED_LABEL_REPRINT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelReprintCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelReprintCount is not null
        defaultShipmentOrderFiltering("labelReprintCount.specified=true", "labelReprintCount.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelReprintCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelReprintCount is greater than or equal to
        defaultShipmentOrderFiltering(
            "labelReprintCount.greaterThanOrEqual=" + DEFAULT_LABEL_REPRINT_COUNT,
            "labelReprintCount.greaterThanOrEqual=" + UPDATED_LABEL_REPRINT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelReprintCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelReprintCount is less than or equal to
        defaultShipmentOrderFiltering(
            "labelReprintCount.lessThanOrEqual=" + DEFAULT_LABEL_REPRINT_COUNT,
            "labelReprintCount.lessThanOrEqual=" + SMALLER_LABEL_REPRINT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelReprintCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelReprintCount is less than
        defaultShipmentOrderFiltering(
            "labelReprintCount.lessThan=" + UPDATED_LABEL_REPRINT_COUNT,
            "labelReprintCount.lessThan=" + DEFAULT_LABEL_REPRINT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByLabelReprintCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where labelReprintCount is greater than
        defaultShipmentOrderFiltering(
            "labelReprintCount.greaterThan=" + SMALLER_LABEL_REPRINT_COUNT,
            "labelReprintCount.greaterThan=" + DEFAULT_LABEL_REPRINT_COUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFailCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where failCount equals to
        defaultShipmentOrderFiltering("failCount.equals=" + DEFAULT_FAIL_COUNT, "failCount.equals=" + UPDATED_FAIL_COUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFailCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where failCount in
        defaultShipmentOrderFiltering(
            "failCount.in=" + DEFAULT_FAIL_COUNT + "," + UPDATED_FAIL_COUNT,
            "failCount.in=" + UPDATED_FAIL_COUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFailCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where failCount is not null
        defaultShipmentOrderFiltering("failCount.specified=true", "failCount.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFailCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where failCount is greater than or equal to
        defaultShipmentOrderFiltering(
            "failCount.greaterThanOrEqual=" + DEFAULT_FAIL_COUNT,
            "failCount.greaterThanOrEqual=" + UPDATED_FAIL_COUNT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFailCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where failCount is less than or equal to
        defaultShipmentOrderFiltering("failCount.lessThanOrEqual=" + DEFAULT_FAIL_COUNT, "failCount.lessThanOrEqual=" + SMALLER_FAIL_COUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFailCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where failCount is less than
        defaultShipmentOrderFiltering("failCount.lessThan=" + UPDATED_FAIL_COUNT, "failCount.lessThan=" + DEFAULT_FAIL_COUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFailCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where failCount is greater than
        defaultShipmentOrderFiltering("failCount.greaterThan=" + SMALLER_FAIL_COUNT, "failCount.greaterThan=" + DEFAULT_FAIL_COUNT);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerCode equals to
        defaultShipmentOrderFiltering("partnerCode.equals=" + DEFAULT_PARTNER_CODE, "partnerCode.equals=" + UPDATED_PARTNER_CODE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerCode in
        defaultShipmentOrderFiltering(
            "partnerCode.in=" + DEFAULT_PARTNER_CODE + "," + UPDATED_PARTNER_CODE,
            "partnerCode.in=" + UPDATED_PARTNER_CODE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerCode is not null
        defaultShipmentOrderFiltering("partnerCode.specified=true", "partnerCode.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerCode contains
        defaultShipmentOrderFiltering("partnerCode.contains=" + DEFAULT_PARTNER_CODE, "partnerCode.contains=" + UPDATED_PARTNER_CODE);
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPartnerCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where partnerCode does not contain
        defaultShipmentOrderFiltering(
            "partnerCode.doesNotContain=" + UPDATED_PARTNER_CODE,
            "partnerCode.doesNotContain=" + DEFAULT_PARTNER_CODE
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentPercentIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentPercent equals to
        defaultShipmentOrderFiltering(
            "paymentPercent.equals=" + DEFAULT_PAYMENT_PERCENT,
            "paymentPercent.equals=" + UPDATED_PAYMENT_PERCENT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentPercentIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentPercent in
        defaultShipmentOrderFiltering(
            "paymentPercent.in=" + DEFAULT_PAYMENT_PERCENT + "," + UPDATED_PAYMENT_PERCENT,
            "paymentPercent.in=" + UPDATED_PAYMENT_PERCENT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentPercentIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentPercent is not null
        defaultShipmentOrderFiltering("paymentPercent.specified=true", "paymentPercent.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentPercentIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentPercent is greater than or equal to
        defaultShipmentOrderFiltering(
            "paymentPercent.greaterThanOrEqual=" + DEFAULT_PAYMENT_PERCENT,
            "paymentPercent.greaterThanOrEqual=" + (DEFAULT_PAYMENT_PERCENT.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentPercentIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentPercent is less than or equal to
        defaultShipmentOrderFiltering(
            "paymentPercent.lessThanOrEqual=" + DEFAULT_PAYMENT_PERCENT,
            "paymentPercent.lessThanOrEqual=" + SMALLER_PAYMENT_PERCENT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentPercentIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentPercent is less than
        defaultShipmentOrderFiltering(
            "paymentPercent.lessThan=" + (DEFAULT_PAYMENT_PERCENT.add(BigDecimal.ONE)),
            "paymentPercent.lessThan=" + DEFAULT_PAYMENT_PERCENT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPaymentPercentIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where paymentPercent is greater than
        defaultShipmentOrderFiltering(
            "paymentPercent.greaterThan=" + SMALLER_PAYMENT_PERCENT,
            "paymentPercent.greaterThan=" + DEFAULT_PAYMENT_PERCENT
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPublicTrackingAllowedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where publicTrackingAllowed equals to
        defaultShipmentOrderFiltering(
            "publicTrackingAllowed.equals=" + DEFAULT_PUBLIC_TRACKING_ALLOWED,
            "publicTrackingAllowed.equals=" + UPDATED_PUBLIC_TRACKING_ALLOWED
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPublicTrackingAllowedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where publicTrackingAllowed in
        defaultShipmentOrderFiltering(
            "publicTrackingAllowed.in=" + DEFAULT_PUBLIC_TRACKING_ALLOWED + "," + UPDATED_PUBLIC_TRACKING_ALLOWED,
            "publicTrackingAllowed.in=" + UPDATED_PUBLIC_TRACKING_ALLOWED
        );
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByPublicTrackingAllowedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        // Get all the shipmentOrderList where publicTrackingAllowed is not null
        defaultShipmentOrderFiltering("publicTrackingAllowed.specified=true", "publicTrackingAllowed.specified=false");
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByIssueIsEqualToSomething() throws Exception {
        OrderIssue issue;
        if (TestUtil.findAll(em, OrderIssue.class).isEmpty()) {
            shipmentOrderRepository.saveAndFlush(shipmentOrder);
            issue = OrderIssueResourceIT.createEntity(em);
        } else {
            issue = TestUtil.findAll(em, OrderIssue.class).get(0);
        }
        em.persist(issue);
        em.flush();
        shipmentOrder.setIssue(issue);
        shipmentOrderRepository.saveAndFlush(shipmentOrder);
        Long issueId = issue.getId();
        // Get all the shipmentOrderList where issue equals to issueId
        defaultShipmentOrderShouldBeFound("issueId.equals=" + issueId);

        // Get all the shipmentOrderList where issue equals to (issueId + 1)
        defaultShipmentOrderShouldNotBeFound("issueId.equals=" + (issueId + 1));
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByReturnRequestIsEqualToSomething() throws Exception {
        OrderReturnRequest returnRequest;
        if (TestUtil.findAll(em, OrderReturnRequest.class).isEmpty()) {
            shipmentOrderRepository.saveAndFlush(shipmentOrder);
            returnRequest = OrderReturnRequestResourceIT.createEntity(em);
        } else {
            returnRequest = TestUtil.findAll(em, OrderReturnRequest.class).get(0);
        }
        em.persist(returnRequest);
        em.flush();
        shipmentOrder.setReturnRequest(returnRequest);
        shipmentOrderRepository.saveAndFlush(shipmentOrder);
        Long returnRequestId = returnRequest.getId();
        // Get all the shipmentOrderList where returnRequest equals to returnRequestId
        defaultShipmentOrderShouldBeFound("returnRequestId.equals=" + returnRequestId);

        // Get all the shipmentOrderList where returnRequest equals to (returnRequestId + 1)
        defaultShipmentOrderShouldNotBeFound("returnRequestId.equals=" + (returnRequestId + 1));
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFareAdjustmentRequestIsEqualToSomething() throws Exception {
        OrderFareAdjustmentRequest fareAdjustmentRequest;
        if (TestUtil.findAll(em, OrderFareAdjustmentRequest.class).isEmpty()) {
            shipmentOrderRepository.saveAndFlush(shipmentOrder);
            fareAdjustmentRequest = OrderFareAdjustmentRequestResourceIT.createEntity(em);
        } else {
            fareAdjustmentRequest = TestUtil.findAll(em, OrderFareAdjustmentRequest.class).get(0);
        }
        em.persist(fareAdjustmentRequest);
        em.flush();
        shipmentOrder.setFareAdjustmentRequest(fareAdjustmentRequest);
        shipmentOrderRepository.saveAndFlush(shipmentOrder);
        Long fareAdjustmentRequestId = fareAdjustmentRequest.getId();
        // Get all the shipmentOrderList where fareAdjustmentRequest equals to fareAdjustmentRequestId
        defaultShipmentOrderShouldBeFound("fareAdjustmentRequestId.equals=" + fareAdjustmentRequestId);

        // Get all the shipmentOrderList where fareAdjustmentRequest equals to (fareAdjustmentRequestId + 1)
        defaultShipmentOrderShouldNotBeFound("fareAdjustmentRequestId.equals=" + (fareAdjustmentRequestId + 1));
    }

    @Test
    @Transactional
    void getAllShipmentOrdersBySenderCustomerIsEqualToSomething() throws Exception {
        Customer senderCustomer;
        if (TestUtil.findAll(em, Customer.class).isEmpty()) {
            shipmentOrderRepository.saveAndFlush(shipmentOrder);
            senderCustomer = CustomerResourceIT.createEntity();
        } else {
            senderCustomer = TestUtil.findAll(em, Customer.class).get(0);
        }
        em.persist(senderCustomer);
        em.flush();
        shipmentOrder.setSenderCustomer(senderCustomer);
        shipmentOrderRepository.saveAndFlush(shipmentOrder);
        Long senderCustomerId = senderCustomer.getId();
        // Get all the shipmentOrderList where senderCustomer equals to senderCustomerId
        defaultShipmentOrderShouldBeFound("senderCustomerId.equals=" + senderCustomerId);

        // Get all the shipmentOrderList where senderCustomer equals to (senderCustomerId + 1)
        defaultShipmentOrderShouldNotBeFound("senderCustomerId.equals=" + (senderCustomerId + 1));
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFromOfficeIsEqualToSomething() throws Exception {
        Office fromOffice;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            shipmentOrderRepository.saveAndFlush(shipmentOrder);
            fromOffice = OfficeResourceIT.createEntity();
        } else {
            fromOffice = TestUtil.findAll(em, Office.class).get(0);
        }
        em.persist(fromOffice);
        em.flush();
        shipmentOrder.setFromOffice(fromOffice);
        shipmentOrderRepository.saveAndFlush(shipmentOrder);
        Long fromOfficeId = fromOffice.getId();
        // Get all the shipmentOrderList where fromOffice equals to fromOfficeId
        defaultShipmentOrderShouldBeFound("fromOfficeId.equals=" + fromOfficeId);

        // Get all the shipmentOrderList where fromOffice equals to (fromOfficeId + 1)
        defaultShipmentOrderShouldNotBeFound("fromOfficeId.equals=" + (fromOfficeId + 1));
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByToOfficeIsEqualToSomething() throws Exception {
        Office toOffice;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            shipmentOrderRepository.saveAndFlush(shipmentOrder);
            toOffice = OfficeResourceIT.createEntity();
        } else {
            toOffice = TestUtil.findAll(em, Office.class).get(0);
        }
        em.persist(toOffice);
        em.flush();
        shipmentOrder.setToOffice(toOffice);
        shipmentOrderRepository.saveAndFlush(shipmentOrder);
        Long toOfficeId = toOffice.getId();
        // Get all the shipmentOrderList where toOffice equals to toOfficeId
        defaultShipmentOrderShouldBeFound("toOfficeId.equals=" + toOfficeId);

        // Get all the shipmentOrderList where toOffice equals to (toOfficeId + 1)
        defaultShipmentOrderShouldNotBeFound("toOfficeId.equals=" + (toOfficeId + 1));
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByHubOfficeIsEqualToSomething() throws Exception {
        Office hubOffice;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            shipmentOrderRepository.saveAndFlush(shipmentOrder);
            hubOffice = OfficeResourceIT.createEntity();
        } else {
            hubOffice = TestUtil.findAll(em, Office.class).get(0);
        }
        em.persist(hubOffice);
        em.flush();
        shipmentOrder.setHubOffice(hubOffice);
        shipmentOrderRepository.saveAndFlush(shipmentOrder);
        Long hubOfficeId = hubOffice.getId();
        // Get all the shipmentOrderList where hubOffice equals to hubOfficeId
        defaultShipmentOrderShouldBeFound("hubOfficeId.equals=" + hubOfficeId);

        // Get all the shipmentOrderList where hubOffice equals to (hubOfficeId + 1)
        defaultShipmentOrderShouldNotBeFound("hubOfficeId.equals=" + (hubOfficeId + 1));
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByFinalToOfficeIsEqualToSomething() throws Exception {
        Office finalToOffice;
        if (TestUtil.findAll(em, Office.class).isEmpty()) {
            shipmentOrderRepository.saveAndFlush(shipmentOrder);
            finalToOffice = OfficeResourceIT.createEntity();
        } else {
            finalToOffice = TestUtil.findAll(em, Office.class).get(0);
        }
        em.persist(finalToOffice);
        em.flush();
        shipmentOrder.setFinalToOffice(finalToOffice);
        shipmentOrderRepository.saveAndFlush(shipmentOrder);
        Long finalToOfficeId = finalToOffice.getId();
        // Get all the shipmentOrderList where finalToOffice equals to finalToOfficeId
        defaultShipmentOrderShouldBeFound("finalToOfficeId.equals=" + finalToOfficeId);

        // Get all the shipmentOrderList where finalToOffice equals to (finalToOfficeId + 1)
        defaultShipmentOrderShouldNotBeFound("finalToOfficeId.equals=" + (finalToOfficeId + 1));
    }

    @Test
    @Transactional
    void getAllShipmentOrdersByCurrentTripIsEqualToSomething() throws Exception {
        Trip currentTrip;
        if (TestUtil.findAll(em, Trip.class).isEmpty()) {
            shipmentOrderRepository.saveAndFlush(shipmentOrder);
            currentTrip = TripResourceIT.createEntity(em);
        } else {
            currentTrip = TestUtil.findAll(em, Trip.class).get(0);
        }
        em.persist(currentTrip);
        em.flush();
        shipmentOrder.setCurrentTrip(currentTrip);
        shipmentOrderRepository.saveAndFlush(shipmentOrder);
        Long currentTripId = currentTrip.getId();
        // Get all the shipmentOrderList where currentTrip equals to currentTripId
        defaultShipmentOrderShouldBeFound("currentTripId.equals=" + currentTripId);

        // Get all the shipmentOrderList where currentTrip equals to (currentTripId + 1)
        defaultShipmentOrderShouldNotBeFound("currentTripId.equals=" + (currentTripId + 1));
    }

    private void defaultShipmentOrderFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultShipmentOrderShouldBeFound(shouldBeFound);
        defaultShipmentOrderShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultShipmentOrderShouldBeFound(String filter) throws Exception {
        restShipmentOrderMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(shipmentOrder.getId().intValue())))
            .andExpect(jsonPath("$.[*].orderCode").value(hasItem(DEFAULT_ORDER_CODE)))
            .andExpect(jsonPath("$.[*].draftCode").value(hasItem(DEFAULT_DRAFT_CODE)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].forwardStage").value(hasItem(DEFAULT_FORWARD_STAGE.toString())))
            .andExpect(jsonPath("$.[*].returnStage").value(hasItem(DEFAULT_RETURN_STAGE.toString())))
            .andExpect(jsonPath("$.[*].paymentTerm").value(hasItem(DEFAULT_PAYMENT_TERM.toString())))
            .andExpect(jsonPath("$.[*].goodsType").value(hasItem(DEFAULT_GOODS_TYPE.toString())))
            .andExpect(jsonPath("$.[*].serviceType").value(hasItem(DEFAULT_SERVICE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].senderName").value(hasItem(DEFAULT_SENDER_NAME)))
            .andExpect(jsonPath("$.[*].senderPhone").value(hasItem(DEFAULT_SENDER_PHONE)))
            .andExpect(jsonPath("$.[*].receiverName").value(hasItem(DEFAULT_RECEIVER_NAME)))
            .andExpect(jsonPath("$.[*].receiverPhone").value(hasItem(DEFAULT_RECEIVER_PHONE)))
            .andExpect(jsonPath("$.[*].deliveryAddress").value(hasItem(DEFAULT_DELIVERY_ADDRESS)))
            .andExpect(jsonPath("$.[*].pickupAddress").value(hasItem(DEFAULT_PICKUP_ADDRESS)))
            .andExpect(jsonPath("$.[*].homePickup").value(hasItem(DEFAULT_HOME_PICKUP)))
            .andExpect(jsonPath("$.[*].homeDelivery").value(hasItem(DEFAULT_HOME_DELIVERY)))
            .andExpect(jsonPath("$.[*].qrDropOff").value(hasItem(DEFAULT_QR_DROP_OFF)))
            .andExpect(jsonPath("$.[*].pickupStaffUsername").value(hasItem(DEFAULT_PICKUP_STAFF_USERNAME)))
            .andExpect(jsonPath("$.[*].pickingAt").value(hasItem(DEFAULT_PICKING_AT.toString())))
            .andExpect(jsonPath("$.[*].pickedUpAt").value(hasItem(DEFAULT_PICKED_UP_AT.toString())))
            .andExpect(jsonPath("$.[*].receiverActualName").value(hasItem(DEFAULT_RECEIVER_ACTUAL_NAME)))
            .andExpect(jsonPath("$.[*].receiverActualPhone").value(hasItem(DEFAULT_RECEIVER_ACTUAL_PHONE)))
            .andExpect(jsonPath("$.[*].weightKg").value(hasItem(sameNumber(DEFAULT_WEIGHT_KG))))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].dimensionsText").value(hasItem(DEFAULT_DIMENSIONS_TEXT)))
            .andExpect(jsonPath("$.[*].fareAmount").value(hasItem(sameNumber(DEFAULT_FARE_AMOUNT))))
            .andExpect(jsonPath("$.[*].pickupFeeAmount").value(hasItem(sameNumber(DEFAULT_PICKUP_FEE_AMOUNT))))
            .andExpect(jsonPath("$.[*].deliveryFeeAmount").value(hasItem(sameNumber(DEFAULT_DELIVERY_FEE_AMOUNT))))
            .andExpect(jsonPath("$.[*].partnerFeeAmount").value(hasItem(sameNumber(DEFAULT_PARTNER_FEE_AMOUNT))))
            .andExpect(jsonPath("$.[*].paidAmount").value(hasItem(sameNumber(DEFAULT_PAID_AMOUNT))))
            .andExpect(jsonPath("$.[*].shelfNumber").value(hasItem(DEFAULT_SHELF_NUMBER)))
            .andExpect(jsonPath("$.[*].note").value(hasItem(DEFAULT_NOTE)))
            .andExpect(jsonPath("$.[*].cancelReason").value(hasItem(DEFAULT_CANCEL_REASON)))
            .andExpect(jsonPath("$.[*].labelPrintedAt").value(hasItem(DEFAULT_LABEL_PRINTED_AT.toString())))
            .andExpect(jsonPath("$.[*].labelReprintCount").value(hasItem(DEFAULT_LABEL_REPRINT_COUNT)))
            .andExpect(jsonPath("$.[*].failCount").value(hasItem(DEFAULT_FAIL_COUNT)))
            .andExpect(jsonPath("$.[*].partnerCode").value(hasItem(DEFAULT_PARTNER_CODE)))
            .andExpect(jsonPath("$.[*].paymentPercent").value(hasItem(sameNumber(DEFAULT_PAYMENT_PERCENT))))
            .andExpect(jsonPath("$.[*].publicTrackingAllowed").value(hasItem(DEFAULT_PUBLIC_TRACKING_ALLOWED)));

        // Check, that the count call also returns 1
        restShipmentOrderMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultShipmentOrderShouldNotBeFound(String filter) throws Exception {
        restShipmentOrderMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restShipmentOrderMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingShipmentOrder() throws Exception {
        // Get the shipmentOrder
        restShipmentOrderMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingShipmentOrder() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the shipmentOrder
        ShipmentOrder updatedShipmentOrder = shipmentOrderRepository.findById(shipmentOrder.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedShipmentOrder are not directly saved in db
        em.detach(updatedShipmentOrder);
        updatedShipmentOrder
            .orderCode(UPDATED_ORDER_CODE)
            .draftCode(UPDATED_DRAFT_CODE)
            .status(UPDATED_STATUS)
            .forwardStage(UPDATED_FORWARD_STAGE)
            .returnStage(UPDATED_RETURN_STAGE)
            .paymentTerm(UPDATED_PAYMENT_TERM)
            .goodsType(UPDATED_GOODS_TYPE)
            .serviceType(UPDATED_SERVICE_TYPE)
            .senderName(UPDATED_SENDER_NAME)
            .senderPhone(UPDATED_SENDER_PHONE)
            .receiverName(UPDATED_RECEIVER_NAME)
            .receiverPhone(UPDATED_RECEIVER_PHONE)
            .deliveryAddress(UPDATED_DELIVERY_ADDRESS)
            .pickupAddress(UPDATED_PICKUP_ADDRESS)
            .homePickup(UPDATED_HOME_PICKUP)
            .homeDelivery(UPDATED_HOME_DELIVERY)
            .qrDropOff(UPDATED_QR_DROP_OFF)
            .pickupStaffUsername(UPDATED_PICKUP_STAFF_USERNAME)
            .pickingAt(UPDATED_PICKING_AT)
            .pickedUpAt(UPDATED_PICKED_UP_AT)
            .receiverActualName(UPDATED_RECEIVER_ACTUAL_NAME)
            .receiverActualPhone(UPDATED_RECEIVER_ACTUAL_PHONE)
            .weightKg(UPDATED_WEIGHT_KG)
            .quantity(UPDATED_QUANTITY)
            .dimensionsText(UPDATED_DIMENSIONS_TEXT)
            .fareAmount(UPDATED_FARE_AMOUNT)
            .pickupFeeAmount(UPDATED_PICKUP_FEE_AMOUNT)
            .deliveryFeeAmount(UPDATED_DELIVERY_FEE_AMOUNT)
            .partnerFeeAmount(UPDATED_PARTNER_FEE_AMOUNT)
            .paidAmount(UPDATED_PAID_AMOUNT)
            .shelfNumber(UPDATED_SHELF_NUMBER)
            .note(UPDATED_NOTE)
            .cancelReason(UPDATED_CANCEL_REASON)
            .labelPrintedAt(UPDATED_LABEL_PRINTED_AT)
            .labelReprintCount(UPDATED_LABEL_REPRINT_COUNT)
            .failCount(UPDATED_FAIL_COUNT)
            .partnerCode(UPDATED_PARTNER_CODE)
            .paymentPercent(UPDATED_PAYMENT_PERCENT)
            .publicTrackingAllowed(UPDATED_PUBLIC_TRACKING_ALLOWED);
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(updatedShipmentOrder);

        restShipmentOrderMockMvc
            .perform(
                put(ENTITY_API_URL_ID, shipmentOrderDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(shipmentOrderDTO))
            )
            .andExpect(status().isOk());

        // Validate the ShipmentOrder in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedShipmentOrderToMatchAllProperties(updatedShipmentOrder);
    }

    @Test
    @Transactional
    void putNonExistingShipmentOrder() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shipmentOrder.setId(longCount.incrementAndGet());

        // Create the ShipmentOrder
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restShipmentOrderMockMvc
            .perform(
                put(ENTITY_API_URL_ID, shipmentOrderDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(shipmentOrderDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ShipmentOrder in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchShipmentOrder() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shipmentOrder.setId(longCount.incrementAndGet());

        // Create the ShipmentOrder
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShipmentOrderMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(shipmentOrderDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ShipmentOrder in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamShipmentOrder() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shipmentOrder.setId(longCount.incrementAndGet());

        // Create the ShipmentOrder
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShipmentOrderMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ShipmentOrder in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateShipmentOrderWithPatch() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the shipmentOrder using partial update
        ShipmentOrder partialUpdatedShipmentOrder = new ShipmentOrder();
        partialUpdatedShipmentOrder.setId(shipmentOrder.getId());

        partialUpdatedShipmentOrder
            .draftCode(UPDATED_DRAFT_CODE)
            .status(UPDATED_STATUS)
            .goodsType(UPDATED_GOODS_TYPE)
            .senderPhone(UPDATED_SENDER_PHONE)
            .pickupAddress(UPDATED_PICKUP_ADDRESS)
            .qrDropOff(UPDATED_QR_DROP_OFF)
            .receiverActualPhone(UPDATED_RECEIVER_ACTUAL_PHONE)
            .dimensionsText(UPDATED_DIMENSIONS_TEXT)
            .fareAmount(UPDATED_FARE_AMOUNT)
            .shelfNumber(UPDATED_SHELF_NUMBER)
            .note(UPDATED_NOTE)
            .cancelReason(UPDATED_CANCEL_REASON)
            .labelPrintedAt(UPDATED_LABEL_PRINTED_AT)
            .labelReprintCount(UPDATED_LABEL_REPRINT_COUNT)
            .publicTrackingAllowed(UPDATED_PUBLIC_TRACKING_ALLOWED);

        restShipmentOrderMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedShipmentOrder.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedShipmentOrder))
            )
            .andExpect(status().isOk());

        // Validate the ShipmentOrder in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertShipmentOrderUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedShipmentOrder, shipmentOrder),
            getPersistedShipmentOrder(shipmentOrder)
        );
    }

    @Test
    @Transactional
    void fullUpdateShipmentOrderWithPatch() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the shipmentOrder using partial update
        ShipmentOrder partialUpdatedShipmentOrder = new ShipmentOrder();
        partialUpdatedShipmentOrder.setId(shipmentOrder.getId());

        partialUpdatedShipmentOrder
            .orderCode(UPDATED_ORDER_CODE)
            .draftCode(UPDATED_DRAFT_CODE)
            .status(UPDATED_STATUS)
            .forwardStage(UPDATED_FORWARD_STAGE)
            .returnStage(UPDATED_RETURN_STAGE)
            .paymentTerm(UPDATED_PAYMENT_TERM)
            .goodsType(UPDATED_GOODS_TYPE)
            .serviceType(UPDATED_SERVICE_TYPE)
            .senderName(UPDATED_SENDER_NAME)
            .senderPhone(UPDATED_SENDER_PHONE)
            .receiverName(UPDATED_RECEIVER_NAME)
            .receiverPhone(UPDATED_RECEIVER_PHONE)
            .deliveryAddress(UPDATED_DELIVERY_ADDRESS)
            .pickupAddress(UPDATED_PICKUP_ADDRESS)
            .homePickup(UPDATED_HOME_PICKUP)
            .homeDelivery(UPDATED_HOME_DELIVERY)
            .qrDropOff(UPDATED_QR_DROP_OFF)
            .pickupStaffUsername(UPDATED_PICKUP_STAFF_USERNAME)
            .pickingAt(UPDATED_PICKING_AT)
            .pickedUpAt(UPDATED_PICKED_UP_AT)
            .receiverActualName(UPDATED_RECEIVER_ACTUAL_NAME)
            .receiverActualPhone(UPDATED_RECEIVER_ACTUAL_PHONE)
            .weightKg(UPDATED_WEIGHT_KG)
            .quantity(UPDATED_QUANTITY)
            .dimensionsText(UPDATED_DIMENSIONS_TEXT)
            .fareAmount(UPDATED_FARE_AMOUNT)
            .pickupFeeAmount(UPDATED_PICKUP_FEE_AMOUNT)
            .deliveryFeeAmount(UPDATED_DELIVERY_FEE_AMOUNT)
            .partnerFeeAmount(UPDATED_PARTNER_FEE_AMOUNT)
            .paidAmount(UPDATED_PAID_AMOUNT)
            .shelfNumber(UPDATED_SHELF_NUMBER)
            .note(UPDATED_NOTE)
            .cancelReason(UPDATED_CANCEL_REASON)
            .labelPrintedAt(UPDATED_LABEL_PRINTED_AT)
            .labelReprintCount(UPDATED_LABEL_REPRINT_COUNT)
            .failCount(UPDATED_FAIL_COUNT)
            .partnerCode(UPDATED_PARTNER_CODE)
            .paymentPercent(UPDATED_PAYMENT_PERCENT)
            .publicTrackingAllowed(UPDATED_PUBLIC_TRACKING_ALLOWED);

        restShipmentOrderMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedShipmentOrder.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedShipmentOrder))
            )
            .andExpect(status().isOk());

        // Validate the ShipmentOrder in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertShipmentOrderUpdatableFieldsEquals(partialUpdatedShipmentOrder, getPersistedShipmentOrder(partialUpdatedShipmentOrder));
    }

    @Test
    @Transactional
    void patchNonExistingShipmentOrder() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shipmentOrder.setId(longCount.incrementAndGet());

        // Create the ShipmentOrder
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restShipmentOrderMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, shipmentOrderDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(shipmentOrderDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ShipmentOrder in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchShipmentOrder() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shipmentOrder.setId(longCount.incrementAndGet());

        // Create the ShipmentOrder
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShipmentOrderMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(shipmentOrderDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ShipmentOrder in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamShipmentOrder() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        shipmentOrder.setId(longCount.incrementAndGet());

        // Create the ShipmentOrder
        ShipmentOrderDTO shipmentOrderDTO = shipmentOrderMapper.toDto(shipmentOrder);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restShipmentOrderMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(shipmentOrderDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ShipmentOrder in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteShipmentOrder() throws Exception {
        // Initialize the database
        insertedShipmentOrder = shipmentOrderRepository.saveAndFlush(shipmentOrder);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the shipmentOrder
        restShipmentOrderMockMvc
            .perform(delete(ENTITY_API_URL_ID, shipmentOrder.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return shipmentOrderRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected ShipmentOrder getPersistedShipmentOrder(ShipmentOrder shipmentOrder) {
        return shipmentOrderRepository.findById(shipmentOrder.getId()).orElseThrow();
    }

    protected void assertPersistedShipmentOrderToMatchAllProperties(ShipmentOrder expectedShipmentOrder) {
        assertShipmentOrderAllPropertiesEquals(expectedShipmentOrder, getPersistedShipmentOrder(expectedShipmentOrder));
    }

    protected void assertPersistedShipmentOrderToMatchUpdatableProperties(ShipmentOrder expectedShipmentOrder) {
        assertShipmentOrderAllUpdatablePropertiesEquals(expectedShipmentOrder, getPersistedShipmentOrder(expectedShipmentOrder));
    }
}
