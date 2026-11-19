package com.sumup.bank.core.ports.secondary;

import com.sumup.bank.core.models.Transaction;
import java.util.Optional;

public interface TransactionRepository {

  /**
   * @throws com.sumup.bank.core.exceptions.IdempotencyKeyConflictException if another
   *     transaction already stored the same idempotency key
   */
  void insert(Transaction transaction);

  Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
