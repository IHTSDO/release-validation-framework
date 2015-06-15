package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.entity.ReleaseCenter;
import org.ihtsdo.rvf.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import java.util.List;

@Controller
@RequestMapping("/releasecentres")
@Api(value = "Release Centres")
@ApiIgnore //this is being marked as ignore as these services are already provided by SRS
public class ReleaseCentreController {

    @Autowired
    private EntityService entityService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "TBD", notes = "?" )
    public List<ReleaseCenter> getReleaseCentres() {
        return entityService.findAll(ReleaseCenter.class);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "TBD", notes = "?" )
    public ReleaseCenter getReleaseCenter(@PathVariable Long id) {
        return (ReleaseCenter) entityService.find(ReleaseCenter.class, id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "TBD", notes = "?" )
    public ReleaseCenter deleteReleaseCenter(@PathVariable Long id) {
        ReleaseCenter releaseCenter = (ReleaseCenter) entityService.find(ReleaseCenter.class, id);
        entityService.delete(releaseCenter);
        return releaseCenter;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
	@ApiOperation( value = "TBD", notes = "?" )
    public ReleaseCenter createReleaseCenter(ReleaseCenter releaseCenter) {
        return (ReleaseCenter) entityService.create(releaseCenter);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "TBD", notes = "?" )
    public ReleaseCenter updateReleaseCenter(@PathVariable Long id,
                                     @RequestBody(required = false) ReleaseCenter releaseCenter) {
        ReleaseCenter rc = (ReleaseCenter) entityService.find(ReleaseCenter.class, id);
        releaseCenter.setId(rc.getId());
        return (ReleaseCenter) entityService.update(releaseCenter);
    }

    @RequestMapping(value = "count", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "Get assertions for a group",
		notes = "?" )
    public Long countReleaseCenters() {
        return entityService.count(ReleaseCenter.class);
    }

}
