package com.sumup.bank.core.ports.primary;

import com.sumup.bank.core.models.Account;
import com.sumup.bank.core.models.CreateAccountCommand;
import com.sumup.bank.core.models.DepositCommand;
import com.sumup.bank.core.models.Transaction;
import java.util.UUID;

public interface AccountService {

  Account createAccount(CreateAccountCommand command);

  /** @throws com.sumup.bank.core.exceptions.AccountNotFoundException if it does not exist */
  Account getAccount(UUID accountId);

  /** Adds money to an account. Returns the stored deposit, or the original one for a retried key. */
  Transaction deposit(DepositCommand command);
}
