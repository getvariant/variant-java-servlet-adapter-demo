/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.web;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.Vets;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.variant.client.Session;
import com.variant.client.StateRequest;
import com.variant.client.VariantException;
import com.variant.client.servlet.demo.VariantContext;
import com.variant.core.schema.Schema;
import com.variant.core.schema.State;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
public class VetController {

    private static final Logger LOG = LoggerFactory.getLogger(VetController.class);

    private final ClinicService clinicService;

    @Autowired
    public VetController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    @RequestMapping(value={"/vets.xml","/vets.html"})
    public String showVetList(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model) {
        // Here we are returning an object of type 'Vets' rather than a collection of Vet objects 
        // so it is simpler for Object-Xml mapping
        Vets vets = new Vets();
        vets.getVetList().addAll(this.clinicService.findVets());
        model.put("vets", vets);
        
        // Obtain Variant session and target it for the this state.
        Session variantSsn = VariantContext.getSession(request);
        if (variantSsn != null) {
        	try {
        		variantSsn.getAttributes().put("user", String.valueOf(request.getSession().getAttribute("user")));
        		Schema schema = variantSsn.getSchema();
        		State vetsPage = schema.getState("vets").get();
        		StateRequest req = variantSsn.targetForState(vetsPage);
        		req.getLiveExperience(schema.getVariation("VetsHourlyRateFeature").get()).ifPresent(
        				(exp) -> model.put("hourlyRateExperience", exp.getName()));
        		req.getLiveExperience(schema.getVariation("ScheduleVisitTest").get()).ifPresent(
        				(exp) -> model.put("scheduleVisitExperience", exp.getName()));
                req.commit(response); // Should be later
        	}
        	catch (VariantException vex) {
        		variantSsn.getStateRequest().ifPresent((req) -> req.fail(response));
        		LOG.error("Unexpected VariantException", vex);
        	}
        }
        
        return "vets/vetList";
    }
     
    @RequestMapping("/vets.json")
    public @ResponseBody Vets showResourcesVetList() {
        // Here we are returning an object of type 'Vets' rather than a collection of Vet objects 
        // so it is simpler for JSon/Object mapping
        Vets vets = new Vets();
        vets.getVetList().addAll(this.clinicService.findVets());
        return vets;
    }

}
