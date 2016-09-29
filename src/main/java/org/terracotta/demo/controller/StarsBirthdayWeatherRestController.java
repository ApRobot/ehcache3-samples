package org.terracotta.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.terracotta.demo.ResourceCallReport;
import org.terracotta.demo.domain.Actor;
import org.terracotta.demo.domain.ActorAndWeatherAndCallReports;
import org.terracotta.demo.domain.WeatherReport;
import org.terracotta.demo.repository.ActorRepository;
import org.terracotta.demo.service.WeatherService;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.terracotta.demo.controller.StarsBirthdayWeatherController.getResourceCallReports;
import static org.terracotta.demo.controller.StarsBirthdayWeatherController.resourceCallReports;

@RestController
public class StarsBirthdayWeatherRestController {

    private static final String MONTREAL = "Montréal, Québec, Canada";
    private static final String PARIS = "Paris, Ile de France, France";
    private static final String MOSCOW = "Moscow, Russia";
    private static final String TOKYO = "Tokyo, Japan";

    @Autowired
    ActorRepository actorRepository;

    @Autowired
    WeatherService weatherService;

    @RequestMapping(value = "/actors/{id}", produces = "application/json")
    public ActorAndWeatherAndCallReports actorsList(@PathVariable("id") long id) throws UnknownHostException {
        long findStartTime = Clock.systemDefaultZone().millis();
        Actor foundActor = actorRepository.findOne(id);
        long findEndTime = Clock.systemDefaultZone().millis();
        if (foundActor == null) {
            throw new RuntimeException("No actor found with this id : " + id);
        }
        getResourceCallReports().add(new ResourceCallReport(id + "", findEndTime - findStartTime, "ActorRepository.findByLastNameIgnoreCase", ResourceCallReport.ResourceType.DATABASE, "Actor table"));


        List<WeatherReport> weatherReports = new ArrayList<>();
        weatherReports.add(foundActor.getBirthLocation() == null ? new WeatherReport() : weatherService.retrieveWeatherReport(foundActor.getBirthLocation(), foundActor.getBirthDate()));
        weatherReports.add(weatherService.retrieveWeatherReport(MONTREAL, foundActor.getBirthDate()));
        weatherReports.add(weatherService.retrieveWeatherReport(PARIS, foundActor.getBirthDate()));
        weatherReports.add(weatherService.retrieveWeatherReport(MOSCOW, foundActor.getBirthDate()));
        weatherReports.add(weatherService.retrieveWeatherReport(TOKYO, foundActor.getBirthDate()));

        long sum = getResourceCallReports().stream().mapToLong(resourceCallReport -> resourceCallReport.getTimeSpentMillis()).sum();
        ActorAndWeatherAndCallReports actorAndWeatherAndCallReports =
            new ActorAndWeatherAndCallReports(foundActor, weatherReports, getResourceCallReports(), sum, InetAddress.getLocalHost().getHostName());


        resourceCallReports.remove();
        return actorAndWeatherAndCallReports;
    }

    public class ErrorInfo {
        public final String url;
        public final String ex;

        public ErrorInfo(String url, Throwable ex) {
            this.url = url;
            this.ex = ex.getMessage();
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    @ResponseBody ErrorInfo handleBadRequest(HttpServletRequest req, Exception ex) {
        return new ErrorInfo(req.getRequestURL().toString(), ex.getCause());
    }


}
