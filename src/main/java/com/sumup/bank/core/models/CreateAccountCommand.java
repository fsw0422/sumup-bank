package com.sumup.bank.core.models;

import java.util.Currency;

/** @param initialBalance in minor units, zero or more */
public record CreateAccountCommand(String ownerName, Currency currency, long initialBalance) {}
