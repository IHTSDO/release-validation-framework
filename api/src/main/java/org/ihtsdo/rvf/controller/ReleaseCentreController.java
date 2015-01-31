package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/releasecentres")
public class ReleaseCentreController {

    @Autowired
    private EntityService entityService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public List<ReleaseCenter> getReleaseCentres() {
        return entityService.findAll(ReleaseCenter.class);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ReleaseCenter getReleaseCenter(@PathVariable Long id) {
        return (ReleaseCenter) entityService.find(ReleaseCenter.class, id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ReleaseCenter deleteReleaseCenter(@PathVariable Long id) {
        ReleaseCenter releaseCenter = (ReleaseCenter) entityService.find(ReleaseCenter.class, id);
        entityService.delete(releaseCenter);
        return releaseCenter;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public ReleaseCenter createReleaseCenter(ReleaseCenter releaseCenter) {
        return (ReleaseCenter) entityService.create(releaseCenter);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ReleaseCenter updateReleaseCenter(@PathVariable Long id,
                                     @RequestBody(required = false) ReleaseCenter releaseCenter) {
        ReleaseCenter rc = (ReleaseCenter) entityService.find(ReleaseCenter.class, id);
        releaseCenter.setId(rc.getId());
        return (ReleaseCenter) entityService.update(releaseCenter);
    }

    @RequestMapping(value = "count", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Long countReleaseCenters() {
        return entityService.count(ReleaseCenter.class);
    }

}
