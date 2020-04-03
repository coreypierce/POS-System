package root.api.layout.section;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Shift;
import edu.wit.se16.model.Table;
import edu.wit.se16.model.layout.Section;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class RequestAddTable implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer section_id = request.getBody("section_id", Integer::parseInt, null);
		Integer table_id = request.getBody("table_id", Integer::parseInt, null);
		
		// validate parameters
		if(table_id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing 'table_id'");
		}
		
		if(section_id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing 'section_id'");
		}
		
		Shift current_shift = Shift.getCurrentShift();
		if(current_shift == null) {
			LOG.error("No shift found in the system!");
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_CONFLICT, "There is no active shift available");
		}
		
		LOG.trace("Request to add Table #{} into Section #{}...", table_id, section_id);
		
		Table table;
		
		try {
			// lookup table
			table = new Table(table_id);
			
			// check if there is a section (in this shift) that already contains the table
			Section old_section = Section.findSection(current_shift, table);
			if(old_section != null) {
				LOG.trace("Table #{} was already part of Section #{}! Removing table from section...", table_id, old_section.getId());
				old_section.removeTable(table);
			}
			
		} catch(NoSuchElementException e) {
			LOG.error("Requested Table #{} doesn't none exists!", table_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Specified Table does not exist!");
		}
		
		try {
			// query the new Section
			Section section = new Section(section_id);
			section.addTable(table);
			
		} catch(NoSuchElementException e) {
			LOG.error("Requested Section #{} doesn't none exists!", section_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Specified Section does not exist!");
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "add_table"; }
}