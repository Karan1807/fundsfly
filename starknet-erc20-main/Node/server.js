const express = require('express');
const starknet = require("starknet");
const fs = require("fs").promises;
const path = require("path");
const erc20ABI = require('../erc20/target/artifacts/erc20.json');

const { Account, Call, Uint256, stark, ec, hash, CallData, RpcProvider, Contract, cairo } = starknet;


const app = express();
const port = 3000;

const contractAddress = "0x03a5de3cd67b053552ef90cf008db8eec1baacf250760690d44784257fdbda5a";

function connectToStarknet() {
    return new RpcProvider({
      nodeUrl: "https://starknet-sepolia.g.alchemy.com/v2/BwG1aH8m0jxH6x5eKSUelvx5nwXamWgX",
    });
}

let provider = connectToStarknet();
let balance = '';

const privateKey = '0x04367631b29884958069d8b9e2b9e62883018f2f0dd79da84d8c102387dc1113';
const accountAddress = '0x02eC573B46b787989f063748D2D0D81d60e6888aB6334b52A55652878477f564';

const account0 = new Account(provider, accountAddress, privateKey);

app.use(express.json());

const resp =   provider.getSpecVersion().then((result) => {
    console.log('rpc version =', result)
})


app.get('/newaccount', async (req, res) => {
  try {
    const privateKey = stark.randomAddress();
    console.log('New OZ account:\nprivateKey=', privateKey);
    const starkKeyPub = ec.starkCurve.getStarkKey(privateKey);
    console.log('publicKey=', starkKeyPub);

    const OZaccountClassHash = '0x02eC573B46b787989f063748D2D0D81d60e6888aB6334b52A55652878477f564';
    // Calculate future address of the account
    const OZaccountConstructorCallData = CallData.compile({ publicKey: starkKeyPub });
    const OZcontractAddress = hash.calculateContractAddressFromHash(
    starkKeyPub,
    OZaccountClassHash,
    OZaccountConstructorCallData,
    0
    );
    console.log('Precalculated account address=', OZcontractAddress);


    res.status(200).json({
          address: OZcontractAddress,
          privateKey: privateKey,
      });

  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});


app.post('/balance', async (req, res) => {
    const { address } = req.body;
  try {
    const contract = new Contract(erc20ABI, contractAddress, provider);
    const data = await contract.balance_of(address);
    balance = data.toString();
    res.json({ message: balance });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/transfer', async (req, res) => {
  try {
    const { recipient, amount } = req.body;
    const erc20 = new Contract(erc20ABI, contractAddress, account0);
     
    erc20.connect(account0)
    // await erc20.transfer( recipient, amount);

    const toTransferTk = cairo.uint256(amount);
    console.log("adsa: ",toTransferTk)
    const transferCallData = erc20.populate('transfer', {
    recipient: recipient,
    amount: toTransferTk, // with Cairo 1 contract, 'toTransferTk' can be replaced by '10n'
    });
    const { transaction_hash: transferTxHash } = await erc20.transfer(transferCallData.calldata);

    // Wait for the invoke transaction to be accepted on Starknet
    console.log(`Waiting for Tx to be Accepted on Starknet - Transfer...`);
    await provider.waitForTransaction(transferTxHash);


    res.json({ message: "Transfer successful" });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
