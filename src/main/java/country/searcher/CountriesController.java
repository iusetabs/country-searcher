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
    private Map<String, Double> loc;
    private Map<Double, List<Country>> countriesAsAMap;
    private ArrayList<Country> countriesInOrder;

    public CountriesController(){
        processUserLocation(get("http://api.ipstack.com/check?access_key=a1d5abe0fd6709ed6ee80744cc29def2"));
        System.out.println("DEBUG: " + this.loc.get("lat"));
        System.out.println("DEBUG: " + this.loc.get("lng"));
        this.countriesAsAMap = processKnownCountries("countries_metadata.json");
        System.out.println("DEBUG: " + this.countriesAsAMap);
        this.countriesInOrder = arrangeMapByNumbers(this.countriesAsAMap);
        System.out.println("DEBUG: " + this.countriesInOrder);

    }

    @CrossOrigin
    @RequestMapping(path="/countries", method=POST)
    public @ResponseBody
    ArrayList<Country> getNearestCountry(@RequestBody String c) throws ExecutionException, InterruptedException {
        System.out.println(loc);
        Future<ArrayList<Country>> future = asyncGetNearestCountry(c);
        while (true) {
            if (future.isDone()) {
                return future.get();
            }
            Thread.sleep(10);
        }
    }
    @Async("threadPoolTaskExecutor")
    public Future<ArrayList<Country>> asyncGetNearestCountry(String c){
       System.out.println("Received from client" + c);
       ArrayList<Country> al = new ArrayList<>();
       al.add(new Country());
       return new AsyncResult<>(al); //Placeholder
    }

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

    public double calculateLatLngDif(Country c){
        return distance(c.getLat(), this.loc.get("lat"), c.getLng(), this.loc.get("lng"));
    }

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

    public void processUserLocation(String loc){
        JsonFactory jsonfactory = new JsonFactory(); //init factory
        this.loc = new HashMap<>();
        try {
            JsonParser jsonParser = jsonfactory.createParser(new StringReader(loc)); //create JSON parser
            JsonToken jsonToken = jsonParser.nextToken();
            while (jsonToken!= JsonToken.END_ARRAY) { //Iterate all elements of array
                String fieldname = jsonParser.getCurrentName(); //get current name of token
                System.out.println(fieldname);
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

    public Map<Double, List<Country>> processKnownCountries(String jsonFilePath){
        Map<Double, List<Country>> countries = new HashMap<>();
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
                while(jsonToken!=JsonToken.FIELD_NAME && jsonToken!=JsonToken.END_OBJECT )
                    jsonToken = jsonParser.nextToken(); //read next token
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
                    //do some processing, Indexing, saving in DB etc..
                    double distanceFromCountry = calculateLatLngDif(country);
                    if(!countries.containsKey(distanceFromCountry))
                        countries.put(distanceFromCountry, new ArrayList<>());
                    countries.get(distanceFromCountry).add(country);
                    country = new Country();
                    numberOfRecords++;
                }
                jsonToken = jsonParser.nextToken();
            }
            long finish = System.currentTimeMillis();
            System.out.println("Time taken to run processKnownCountries : " + TimeUnit.MILLISECONDS.toSeconds(finish - start));
            System.out.println("Total Records Found : "+numberOfRecords);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return countries;
    }

    public ArrayList<Country> arrangeMapByNumbers(Map<Double, List<Country>> m){
        List<Double> sortedKeys = new ArrayList<>(m.keySet());
        ArrayList<Country> c = new ArrayList<>();
        long start = System.currentTimeMillis();
        Collections.sort(sortedKeys);
        for (Double key : sortedKeys){
            c.addAll(m.get(key));
        }
        long finish = System.currentTimeMillis();
        System.out.println("Time taken to run processKnownCountries : " + TimeUnit.MILLISECONDS.toSeconds(finish - start));
        return c;
    }
}
