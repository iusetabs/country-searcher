# country-searcher
![The earth](https://scet.berkeley.edu/wp-content/uploads/Global-Innovation-Collider-Image-1.jpg)
## Whats in the box? 
### Client code: Location at /client/
  + [index.html](/client/index.html) - run in your browser. 
  + [admin-main.js](/client/admin-main.js) - Jquery code. Listeners on input, dynamic adding of suggestions and POST ajax. 
  + [stylesheet.css](/client/stylesheet.css) - Neater presentation and minor Bootstrap fixes. 
### Server code: Location at /src/main/java/country/searcher/
  + [App.java](/src/main/java/country/searcher/App.java) - The executable for the REST service.
  + [CountriesController.java](/src/main/java/country/searcher/CountriesController.java) - The main controller. The REST endpoint. Where all the magic happens. 
  + [Country.java](/src/main/java/country/searcher/Country.java) - Vanilla country object. 
  + [SpringAsyncConfig.java](/src/main/java/country/searcher/SpringAsyncConfig.java) - Required to run endpoints on seperate threads per connection. 
  
## What sauce did I use? 
  ### Server
  + Java Spring 
  + Java 11
  ### Client 
  + Jquery
  + HTML
  + CSS 
  + Bootstrap
  
## Some quick info
  + Gradle used for dependency management. 
  + Server coded in Java Spring ( REST ). 
  + IDE's used - Atom ( Client ) & Intellij ( Server ). 
  + Mostly core libraries used. 
  + Used custom lat / lng function. Credited in comments. 
  + Used jackson JSON library. 
  + Used Bootstrap for client. 
  
## How to run? 
### Server code
  + Run ./run_on_localhost8080.sh located at project root. 
  + Alternatively, run "java -jar build/libs/country-searcher-0.1.0.jar" at the root directory.
  + If using .sh script, server gets logged to "rest_log.txt". Use tail -F rest_log.txt to follow output. 
  + REST Server will run on port 8080. The client will post to the /countries endpoint. Only a POST endpoint has been set up. 
### Client code 
  + Drag index.html into your favourite browser! 
  
## Some issues I ran into: 
  + A CORS issue with the client and the server. I fixed this by replacing localhost with http://127.0.0.1 in client. 
  + REST Server wanted a database. Annotion exclude={DataSourceAutoConfiguration.class} in "App.java" removes this error. 
  
# References
 + [Stackoverflow](https://stackoverflow.com)
 + [Baeldung](www.baeldung.com)
 + [Orcale docs](https://docs.oracle.com)
