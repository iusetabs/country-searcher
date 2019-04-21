package country.searcher;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Configuration
@EnableAsync
@Controller
@RestController
public class CountriesController {
    @Bean(name = "threadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }

    //A map representing location details of the server
    private Map<String, Double> loc;

    //The JSON file of countries loaded as a java object. Key is the country's distance from the server.
    private Map<Double, List<Country>> countriesAsAMap;

    //An arraylist of country objects, ordered by distance from the server
    private ArrayList<Country> countriesInOrder;

    //@Info loads this on server boot.
    public CountriesController(){
        processUserLocation(get("http://api.ipstack.com/check?access_key=a1d5abe0fd6709ed6ee80744cc29def2"));
        this.countriesAsAMap = processKnownCountries("countries_metadata.json");
        this.countriesInOrder = arrangeMapByNumbers(this.countriesAsAMap);
    }

    //Endpoint for countries post.
    @CrossOrigin
    @RequestMapping(path="/countries", method=POST)
    public @ResponseBody
    ArrayList<String> getNearestCountry(@RequestBody String inputFragment) throws ExecutionException, InterruptedException {
        Future<ArrayList<String>> future = asyncGetNearestCountry(inputFragment);
        while (true) {
            if (future.isDone()) {
                return future.get();
            }
            Thread.sleep(10);
        }
    }
    //Run process on seperate thread.
    @Async("threadPoolTaskExecutor")
    public Future<ArrayList<String>> asyncGetNearestCountry(String inputFragment){
       return new AsyncResult<>(matchClosestCountriesWithString(inputFragment, 5));
    }

    /*
    @Param: inputFragment: The string snippet from the client.
    @Param: i: The number of countries returned to the client. Default is 5.
    @Info: Returns the countries closest to the user, which start with inputFragment.
    @Info: Calls various sub-methods to aid this process.
    @Return: An arraylist of strings, ordered with closest to the user input.
 */
    private ArrayList<String> matchClosestCountriesWithString(String inputFragment, int i) {
        int j = 0; //Inner counter
        inputFragment = inputFragment.toLowerCase();
        ArrayList<String>  orderedAL = new ArrayList<>();
        for(Country country : this.countriesInOrder){
            if(j < i){ //If inner counter reaches the max number of countries client wants
                if(country.getName().toLowerCase().startsWith(inputFragment)){
                    j+=1;
                    orderedAL.add(country.getName());
                }
            }
            else
                break;
        }
        return orderedAL;
    }
    /*
         @Param: country: the country object we want to compare with the server location.
         @Info: Calls a lat, lng distance method.
         @Return: The distance as a double.
      */
    public double calculateLatLngDif(Country country){
        return distance(country.getLat(), this.loc.get("lat"), country.getLng(), this.loc.get("lng"));
    }

    /*
        @Param: locAsJson: The location as a JSON String.
        @Info: Converts the JSON string to a java object.
        @Info: location is stored in the Map "this.loc"
        @Void function
     */
    public void processUserLocation(String locAsJson){
        JsonFactory jsonfactory = new JsonFactory(); //init factory
        this.loc = new HashMap<>();
        try {
            JsonParser jsonParser = jsonfactory.createParser(new StringReader(locAsJson)); //create JSON parser
            JsonToken jsonToken = jsonParser.nextToken();
            while (jsonToken!= JsonToken.END_ARRAY) { //Iterate all elements of array
                String fieldname = jsonParser.getCurrentName(); //get current name of token
                if ("longitude".equals(fieldname)) {
                    jsonToken = jsonParser.nextToken(); //read next token
                    this.loc.put("lng", jsonParser.getDoubleValue());
                }
                if ("latitude".equals(fieldname)) {
                    jsonToken = jsonParser.nextToken(); //read next token
                    this.loc.put("lat", jsonParser.getDoubleValue());
                }
                jsonToken = jsonParser.nextToken(); //read next token
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
        @Param: jsonFilePath: The file path to the JSON file.
        @Info: Creates a MAP of Lists. The key is the distance of the object from the server location.
        @Info: Lists contain country objects.
        @Info: Lists are used in the case of two distances being the same.
        @Return: Map of Lists.
     */
    public Map<Double, List<Country>> processKnownCountries(String jsonFilePath){
        Map<Double, List<Country>> countries = new HashMap<>(); //Object to return
        File jsonFile = new File(jsonFilePath);
        JsonFactory jsonfactory = new JsonFactory(); //init factory
        try {
            int numberOfRecords = 0;
            JsonParser jsonParser = jsonfactory.createParser(jsonFile); //create JSON parser
            Country country = new Country();
            JsonToken jsonToken = jsonParser.nextToken();
            long start = System.currentTimeMillis();
            while (jsonToken!= JsonToken.END_ARRAY){ //Iterate all elements of array
                String fieldname = jsonParser.getCurrentName(); //get current name of token
                if ("name".equals(fieldname)) {
                    jsonToken = jsonParser.nextToken(); //read next token
                    country.setName(jsonParser.getText());
                }
                else if ("lat".equals(fieldname)) {
                    jsonToken = jsonParser.nextToken();
                    country.setLat(jsonParser.getDoubleValue());
                }
                else if ("lng".equals(fieldname)) {
                    jsonToken = jsonParser.nextToken();
                    country.setLng(jsonParser.getDoubleValue());
                }
                else if(jsonToken==JsonToken.END_OBJECT){
                    double distanceFromCountry = calculateLatLngDif(country);
                    if(!countries.containsKey(distanceFromCountry)) //If this distance is not already in the Map.
                        countries.put(distanceFromCountry, new ArrayList<>()); //Create new List at this key.
                    countries.get(distanceFromCountry).add(country);
                    country = new Country();
                    numberOfRecords++;
                }
                jsonToken = jsonParser.nextToken();
            }
            long finish = System.currentTimeMillis();
            //Testing. Some run stats.
            System.out.println("Time taken to run processKnownCountries : " + TimeUnit.MILLISECONDS.toSeconds(finish - start));
            System.out.println("Total countries processed : "+numberOfRecords);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return countries;
    }

    /*
        @Param: jsonAsMap: The JSON file as a map, with key values as distance
        @Info: Creates an arraylist of country objects, ordered by the distance from the user's location.
        @Return: ArrayList<Country> : An ordered ArrayList of country objects.
     */
    public ArrayList<Country> arrangeMapByNumbers(Map<Double, List<Country>> jsonAsMap){
        List<Double> sortedKeys = new ArrayList<>(jsonAsMap.keySet());
        ArrayList<Country> countryObjects = new ArrayList<>();
        long start = System.currentTimeMillis();
        Collections.sort(sortedKeys);
        for (Double key : sortedKeys){
            countryObjects.addAll(jsonAsMap.get(key));
        }
        long finish = System.currentTimeMillis();
        //Testing. Some run stats.
        System.out.println("Time taken to run processKnownCountries : " + TimeUnit.MILLISECONDS.toSeconds(finish - start));
        return countryObjects;
    }

    /*
        @Param: uri: the target uri
        @Info: carries out a simple HTTP GET.
        @Info: used to get the server location.
        @Return: String : The response from the GET.
     */
    public String get(String uri){
        String res = "Fail";
        try {
            URL url = new URL(uri);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            int status = con.getResponseCode();
            System.out.println(status);
            System.out.println(content);
            res = valueOf(content);
            con.disconnect();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return res;
    }

    /*
    @Credit to original author David George, https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude
    @returns Distance in meters.
  */
    public double distance(double lat1, double lat2, double lon1,
                           double lon2) {

        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        distance = Math.pow(distance, 2);
        return Math.sqrt(distance);
    }
}
