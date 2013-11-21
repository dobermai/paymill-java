package com.paymill.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.paymill.Paymill;
import com.paymill.models.Client;
import com.paymill.models.Payment;
import com.paymill.models.Preauthorization;
import com.paymill.models.Transaction;

public class PreauthorizationServiceTest {

  private String                  token    = "098f6bcd4621d373cade4e832627b4f6";
  private Integer                 amount   = 4202;
  private String                  currency = "EUR";
  private Payment                 payment;

  private List<Transaction>       preauthorizations;

  private PreauthorizationService preauthorizationService;
  private TransactionService      transactionService;
  private PaymentService          paymentService;
  private ClientService           clientService;

  @BeforeClass
  public void setUp() {
    Paymill paymill = new Paymill( "255de920504bd07dad2a0bf57822ee40" );
    this.clientService = paymill.getClientService();
    this.preauthorizationService = paymill.getPreauthorizationService();
    this.paymentService = paymill.getPaymentService();
    this.transactionService = paymill.getTransactionService();

    this.preauthorizations = new ArrayList<Transaction>();
    this.payment = this.paymentService.createWithToken( this.token );

  }

  @AfterClass
  public void tearDown() {
    for( Transaction transaction : preauthorizations )
      this.preauthorizationService.delete( transaction );
    Assert.assertEquals( this.preauthorizationService.list().getDataCount(), 0 );
    Assert.assertEquals( this.transactionService.list().getDataCount(), 1 );

    //TODO[VNi]: There is an API error, creating a payment results in 2 payments in paymill
    for( Payment payment : this.paymentService.list().getData() ) {
      this.paymentService.delete( payment );
    }
    for( Client client : this.clientService.list().getData() ) {
      this.clientService.delete( client );
    }
  }

  @Test
  public void testCreateWithToken_shouldSucceed() {
    Transaction transaction = this.preauthorizationService.createWithToken( this.token, this.amount, this.currency );
    this.validateTransaction( transaction );
    Assert.assertEquals( transaction.getDescription(), null );
    this.preauthorizations.add( transaction );
  }

  @Test( dependsOnMethods = "testCreateWithToken_shouldSucceed" )
  public void testCreateWithPayment_shouldSucceed() throws Exception {
    Thread.sleep( 1000 );
    Transaction transaction = this.preauthorizationService.createWithPayment( this.payment, this.amount, this.currency );
    this.validateTransaction( transaction );
    Assert.assertEquals( transaction.getDescription(), null );
    this.preauthorizations.add( transaction );
  }

  @Test( dependsOnMethods = "testListOrderByFilterAmountLessThan" )
  public void testListOrderByOffer() {
    Preauthorization.Order orderDesc = Preauthorization.createOrder().byCreatedAt().desc();
    Preauthorization.Order orderAsc = Preauthorization.createOrder().byCreatedAt().asc();

    List<Preauthorization> preauthorizationDesc = this.preauthorizationService.list( null, orderDesc ).getData();
    Assert.assertEquals( preauthorizationDesc.size(), this.preauthorizations.size() );

    List<Preauthorization> preauthorizationAsc = this.preauthorizationService.list( null, orderAsc ).getData();
    Assert.assertEquals( preauthorizationAsc.size(), this.preauthorizations.size() );

    Assert.assertEquals( preauthorizationDesc.get( 0 ).getId(), preauthorizationAsc.get( 1 ).getId() );
    Assert.assertEquals( preauthorizationDesc.get( 1 ).getId(), preauthorizationAsc.get( 0 ).getId() );
  }

  @Test( dependsOnMethods = "testCreateWithPayment_shouldSucceed" )
  public void testListOrderByFilterAmountGreaterThan() {
    Preauthorization.Filter filter = Preauthorization.createFilter().byAmountGreaterThan( amount - 100 );
    List<Preauthorization> preauthorization = this.preauthorizationService.list( filter, null ).getData();
    Assert.assertEquals( preauthorization.size(), this.preauthorizations.size() );
  }

  @Test( dependsOnMethods = "testListOrderByFilterAmountGreaterThan" )
  public void testListOrderByFilterAmountLessThan() {
    Preauthorization.Filter filter = Preauthorization.createFilter().byAmountLessThan( amount );
    List<Preauthorization> preauthorization = this.preauthorizationService.list( filter, null ).getData();
    Assert.assertEquals( preauthorization.size(), 0 );
  }

  private Transaction validateTransaction( final Transaction transaction ) {
    Assert.assertNotNull( transaction );
    Assert.assertNotNull( transaction.getId() );
    Assert.assertEquals( transaction.getAmount(), this.amount );
    Assert.assertEquals( transaction.getOriginAmount(), Integer.valueOf( this.amount ) );
    Assert.assertEquals( transaction.getCurrency(), this.currency );
    Assert.assertEquals( transaction.getStatus(), Transaction.Status.PREAUTH );
    Assert.assertTrue( BooleanUtils.isFalse( transaction.getLivemode() ) );
    Assert.assertNull( transaction.getRefunds() );
    Assert.assertEquals( transaction.getCurrency(), this.currency );
    Assert.assertNotNull( transaction.getCreatedAt() );
    Assert.assertNotNull( transaction.getUpdatedAt() );
    Assert.assertTrue( BooleanUtils.isFalse( transaction.getFraud() ) );
    Assert.assertNotNull( transaction.getPayment() );
    Assert.assertNotNull( transaction.getClient() );
    Assert.assertNotNull( transaction.getPreauthorization() );
    Assert.assertTrue( transaction.getFees().isEmpty() );
    Assert.assertNull( transaction.getAppId() );
    return transaction;
  }

}
