# transaction-monitor

To start it up:
```
gradle run
```

To create a transaction (only transactions newer than 60 sec will be persisted):
```
curl 'http://localhost:9090/transactions' -XPOST -d '{"amount":300,"timestamp":1506979417000}' -H 'Content-Type: application/json'
```
(The timestamp can be generated with the `date +%s000` command)

To get statistics from transactions in the last 60 seconds:
```
curl 'http://localhost:9090/statistics'
```
