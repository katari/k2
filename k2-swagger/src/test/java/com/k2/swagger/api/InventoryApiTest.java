/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.swagger.api;

import java.util.List;
import java.util.LinkedList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

public class InventoryApiTest implements InventoryApiDelegate {

  private List<InventoryItem> items = new LinkedList<>();

  public InventoryApiController createController() {
    return new InventoryApiController(this);
  }

  public ResponseEntity<Void> addInventory(final InventoryItem inventoryItem) {
    items.add(inventoryItem);
    return new ResponseEntity<Void>(HttpStatus.OK);
  }

  public ResponseEntity<List<InventoryItem>> searchInventory(
      final String searchString, final Integer skip, final Integer limit) {

    return new ResponseEntity<List<InventoryItem>>(items, HttpStatus.OK);
  }
}

