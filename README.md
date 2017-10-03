# Demonstrating Flowable

  * author: Sebastien Mosser
  * revision: 10.17
  * Strongly inspired by the _awesome_ documentation provided by Flowable: 
    * [http://www.flowable.org/docs/userguide/index.html](http://www.flowable.org/docs/userguide/index.html)
  
  
 ## Command line application
 
 ### Compiling and Running the process
 
```
azrael:5A-BPM-Demo mosser$ mvn -q package exec:java
``` 

## Using the REST server

## Staring the server

```
azrael:5A-BPM-Demo mosser$ docker run -p8080:8080 flowable/flowable-rest
```