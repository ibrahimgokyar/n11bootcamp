package com.n11bootcamp.user_service.entity;

import java.util.Set;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

public class ShoppingCart {
    @Id
    private long id;
    private String shoppingCartName;

    @ManyToMany(mappedBy = "products")
    private Set<ShoppingCart> shoppingCarts;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getShoppingCartName() {
        return shoppingCartName;
    }

    public void setShoppingCartName(String shoppingCartName) {
        this.shoppingCartName = shoppingCartName;
    }

}