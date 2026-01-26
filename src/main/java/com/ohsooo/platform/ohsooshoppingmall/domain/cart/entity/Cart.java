package com.ohsooo.platform.ohsooshoppingmall.domain.cart.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "carts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_cart_user", columnNames = {"user_id"})
    }
)
public class Cart {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cart_id")
  private Long cartId;

  /**
   * carts.user_id 에 UNIQUE가 걸려 있으므로
   * "유저당 장바구니 1개" 구조를 엔티티에서도 1:1로 매핑한다.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @OneToMany(
      mappedBy = "cart",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  private List<CartItem> cartItems = new ArrayList<>();

  private Cart(User user) {
    this.user = user;
  }

  public static Cart create(User user) {
    return new Cart(user);
  }

  /**
   * 장바구니에 이미 담긴 상품이면 수량만 증가시키고,
   * 없으면 CartItem을 새로 추가한다.
   */
  public CartItem addOrIncreaseItem(CartItem newItem) {
    for (CartItem ci : cartItems) {
      if (ci.isSameVariant(newItem.getItemVariant().getItemVariantId())) {
        ci.increaseQuantity(newItem.getQuantity());
        return ci;
      }
    }
    newItem.attachTo(this);
    cartItems.add(newItem);
    return newItem;
  }

  /**
   * 특정 variant 장바구니 아이템을 찾는다. (없으면 null)
   */
  public CartItem findItemByVariantId(Long itemVariantId) {
    for (CartItem ci : cartItems) {
      if (ci.isSameVariant(itemVariantId)) return ci;
    }
    return null;
  }

  /**
   * 특정 variant 장바구니 아이템 삭제
   */
  public boolean removeItemByVariantId(Long itemVariantId) {
    CartItem target = findItemByVariantId(itemVariantId);
    if (target == null) return false;

    cartItems.remove(target); // orphanRemoval=true라서 cart_items에서도 삭제됨
    target.detach();
    return true;
  }

  /**
   * 장바구니 비우기
   */
  public void clear() {
    for (CartItem ci : cartItems) {
      ci.detach();
    }
    cartItems.clear();
  }
}
