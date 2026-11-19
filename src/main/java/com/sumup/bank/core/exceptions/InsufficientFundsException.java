package com.sumup.bank.core.exceptions;

import java.util.UUID;

public final class InsufficientFundsException extends BankingException {

  public InsufficientFundsException(UUID accountId) {
    super("Account " + accountId + " does not have enough money for this transfer.");
  }
}
