package com.ohsooo.platform.ohsooshoppingmall.domain.order.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 엔티티(주문 단위).
 * - 한 사용자가 여러 주문을 가질 수 있다. (User : Order = 1:N)
 * - 주문은 여러 주문상품(OrderItem)을 가진다. (Order : OrderItem = 1:N)
 * - 주문 시점의 배송정보(스냅샷)를 함께 저장한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_id")
  private Long orderId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalPrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private OrderStatus status;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // ===== 배송(주문 스냅샷) =====

  @Column(name = "receiver_name", nullable = false, length = 255)
  private String receiverName;

  @Column(name = "receiver_phone", nullable = false, length = 50)
  private String receiverPhone;

  @Column(name = "shipping_address", nullable = false, length = 255)
  private String shippingAddress;

  @Column(name = "shipping_postcode", length = 20)
  private String shippingPostcode;

  @Column(name = "shipping_address_detail", length = 255)
  private String shippingAddressDetail;

  @Column(name = "shipping_request_note", length = 255)
  private String shippingRequestNote;

  // ===== 연관 =====

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> orderItems = new ArrayList<>();

  private Order(
      User user,
      String receiverName,
      String receiverPhone,
      String shippingAddress,
      String shippingPostcode,
      String shippingAddressDetail,
      String shippingRequestNote
  ) {
    this.user = user;
    this.status = OrderStatus.CREATED;
    this.totalPrice = BigDecimal.ZERO;

    this.receiverName = receiverName;
    this.receiverPhone = receiverPhone;
    this.shippingAddress = shippingAddress;
    this.shippingPostcode = shippingPostcode;
    this.shippingAddressDetail = shippingAddressDetail;
    this.shippingRequestNote = shippingRequestNote;
  }

  public static Order create(
      User user,
      String receiverName,
      String receiverPhone,
      String shippingAddress,
      String shippingPostcode,
      String shippingAddressDetail,
      String shippingRequestNote
  ) {
    return new Order(
        user,
        receiverName,
        receiverPhone,
        shippingAddress,
        shippingPostcode,
        shippingAddressDetail,
        shippingRequestNote
    );
  }

  public void addOrderItem(OrderItem orderItem) {
    orderItem.attachTo(this);
    this.orderItems.add(orderItem);
  }

  public void changeStatus(OrderStatus status) {
    if (status == null) return;
    this.status = status;
  }

  public void markPaid() {
    if (this.status != OrderStatus.CREATED) {
      throw new IllegalStateException("Order cannot be marked as PAID from status: " + this.status);
    }
    this.status = OrderStatus.PAID;
  }

  public void recalculateTotalPrice() {
    BigDecimal sum = BigDecimal.ZERO;
    for (OrderItem oi : orderItems) {
      // priceAtPurchase * quantity
      sum = sum.add(
          oi.getPriceAtPurchase().multiply(BigDecimal.valueOf(oi.getQuantity()))
      );
    }
    this.totalPrice = sum;
  }

  public boolean isOwnedBy(Long userId) {
    return this.user != null
        && this.user.getUserId() != null
        && this.user.getUserId().equals(userId);
  }

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }
}
