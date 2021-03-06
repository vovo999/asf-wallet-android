package com.asf.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class TransactionContract implements Parcelable {
  public static final Creator<TransactionContract> CREATOR = new Creator<TransactionContract>() {
    @Override public TransactionContract createFromParcel(Parcel in) {
      return new TransactionContract(in);
    }

    @Override public TransactionContract[] newArray(int size) {
      return new TransactionContract[size];
    }
  };
  public String address;
  public String name;
  public String totalSupply;
  public int decimals;
  public String symbol;

  public TransactionContract() {
  }

  private TransactionContract(Parcel in) {
    address = in.readString();
    name = in.readString();
    totalSupply = in.readString();
    decimals = in.readInt();
    symbol = in.readString();
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel parcel, int i) {
    parcel.writeString(address);
    parcel.writeString(name);
    parcel.writeString(totalSupply);
    parcel.writeInt(decimals);
    parcel.writeString(symbol);
  }
}
