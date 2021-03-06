package com.asf.wallet.repository;

import com.asf.wallet.entity.NetworkInfo;
import com.asf.wallet.entity.Transaction;
import com.asf.wallet.entity.TransactionContract;
import com.asf.wallet.entity.TransactionOperation;
import com.asf.wallet.entity.Wallet;
import com.asf.wallet.repository.entity.RealmTransaction;
import com.asf.wallet.repository.entity.RealmTransactionContract;
import com.asf.wallet.repository.entity.RealmTransactionOperation;
import com.asf.wallet.service.RealmManager;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class TransactionsRealmCache implements TransactionLocalSource {

  private final RealmManager realmManager;

  public TransactionsRealmCache(RealmManager realmManager) {
    this.realmManager = realmManager;
  }

  @Override public Single<Transaction[]> fetchTransaction(NetworkInfo networkInfo, Wallet wallet) {
    return Single.fromCallable(() -> {
      Realm instance = null;
      try {
        instance = realmManager.getRealmInstance(networkInfo, wallet);
        RealmResults<RealmTransaction> list = instance.where(RealmTransaction.class)
            .sort("nonce", Sort.DESCENDING)
            .findAll();
        return convert(list);
      } finally {
        if (instance != null) {
          instance.close();
        }
      }
    });
  }

  @Override public Completable putTransactions(NetworkInfo networkInfo, Wallet wallet,
      Transaction[] transactions) {
    return Completable.fromAction(() -> {
      Realm instance = null;
      try {
        instance = realmManager.getRealmInstance(networkInfo, wallet);
        instance.beginTransaction();
        for (Transaction transaction : transactions) {
          RealmTransaction item = instance.where(RealmTransaction.class)
              .equalTo("hash", transaction.hash)
              .findFirst();
          if (item == null) {
            item = instance.createObject(RealmTransaction.class, transaction.hash);
          }
          fill(instance, item, transaction);
        }
        instance.commitTransaction();
      } catch (Exception ex) {
        if (instance != null) {
          instance.cancelTransaction();
        }
      } finally {
        if (instance != null) {
          instance.close();
        }
      }
    })
        .subscribeOn(Schedulers.io());
  }

  @Override public Single<Transaction> findLast(NetworkInfo networkInfo, Wallet wallet) {
    return Single.fromCallable(() -> {
      Realm realm = null;
      try {
        realm = realmManager.getRealmInstance(networkInfo, wallet);
        return convert(realm.where(RealmTransaction.class)
            .sort("timeStamp", Sort.DESCENDING)
            .findAll()
            .first());
      } finally {
        if (realm != null) {
          realm.close();
        }
      }
    })
        .observeOn(Schedulers.io());
  }

  private void fill(Realm realm, RealmTransaction item, Transaction transaction) {
    item.setError(transaction.error);
    item.setBlockNumber(transaction.blockNumber);
    item.setTimeStamp(transaction.timeStamp);
    item.setNonce(transaction.nonce);
    item.setFrom(transaction.from);
    item.setTo(transaction.to);
    item.setValue(transaction.value);
    item.setGas(transaction.gas);
    item.setGasPrice(transaction.gasPrice);
    item.setInput(transaction.input);
    item.setGasUsed(transaction.gasUsed);

    for (TransactionOperation operation : transaction.operations) {
      RealmTransactionOperation realmOperation =
          realm.createObject(RealmTransactionOperation.class);
      realmOperation.setTransactionId(operation.transactionId);
      realmOperation.setViewType(operation.viewType);
      realmOperation.setFrom(operation.from);
      realmOperation.setTo(operation.to);
      realmOperation.setValue(operation.value);

      RealmTransactionContract realmContract = realm.createObject(RealmTransactionContract.class);
      realmContract.setAddress(operation.contract.address);
      realmContract.setName(operation.contract.name);
      realmContract.setTotalSupply(operation.contract.totalSupply);
      realmContract.setDecimals(operation.contract.decimals);
      realmContract.setSymbol(operation.contract.symbol);

      realmOperation.setContract(realmContract);
      item.getOperations()
          .add(realmOperation);
    }
  }

  private Transaction[] convert(RealmResults<RealmTransaction> items) {
    int len = items.size();
    Transaction[] result = new Transaction[len];
    for (int i = 0; i < len; i++) {
      result[i] = convert(items.get(i));
    }
    return result;
  }

  private Transaction convert(RealmTransaction rawItem) {
    int len = rawItem.getOperations()
        .size();
    TransactionOperation[] operations = new TransactionOperation[len];
    for (int i = 0; i < len; i++) {
      RealmTransactionOperation rawOperation = rawItem.getOperations()
          .get(i);
      if (rawOperation == null) {
        continue;
      }
      TransactionOperation operation = new TransactionOperation();
      operation.transactionId = rawOperation.getTransactionId();
      operation.viewType = rawOperation.getViewType();
      operation.from = rawOperation.getFrom();
      operation.to = rawOperation.getTo();
      operation.value = rawOperation.getValue();
      operation.contract = new TransactionContract();
      operation.contract.address = rawOperation.getContract()
          .getAddress();
      operation.contract.name = rawOperation.getContract()
          .getName();
      operation.contract.totalSupply = rawOperation.getContract()
          .getTotalSupply();
      operation.contract.decimals = rawOperation.getContract()
          .getDecimals();
      operation.contract.symbol = rawOperation.getContract()
          .getSymbol();
      operations[i] = operation;
    }
    return new Transaction(rawItem.getHash(), rawItem.getError(), rawItem.getBlockNumber(),
        rawItem.getTimeStamp(), rawItem.getNonce(), rawItem.getFrom(), rawItem.getTo(),
        rawItem.getValue(), rawItem.getGas(), rawItem.getGasPrice(), rawItem.getInput(),
        rawItem.getGasUsed(), operations);
  }
}
