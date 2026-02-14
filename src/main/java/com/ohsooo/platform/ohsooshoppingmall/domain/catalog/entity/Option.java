package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(         // TODO: DB에 유니크 제약 추가
        name = "options",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"item_id", "type", "value"}
                )
        }
)
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="option_id")
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OptionType type;

    @Column(nullable = false, length = 50)
    private String value;

    public Option(Item item, OptionType type, String value) {
        this.item = item;
        this.type = type;
        this.value = value;
    }
}
