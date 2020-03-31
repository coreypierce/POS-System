package root.api.host;

import java.io.IOException;
import java.sql.PreparedStatement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.database.Database;
import edu.wit.se16.model.Shift;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestSectionSeatCounts implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final PreparedStatement QUERY = Database.prep(
			"SELECT section_number AS 'section', COALESCE(count, 0) AS 'count' FROM sections AS s " +
				"LEFT JOIN ( " + 
					"SELECT s.id, SUM(g.guest_count) AS 'count' FROM ( " + 
						"SELECT g.* FROM ( " + 
							"SELECT table_id, MAX(timestamp) AS last_time FROM table_guest_history GROUP BY table_id) AS l " + 
						"INNER JOIN table_guest_history AS g ON " + 
							"g.table_id = l.table_id AND " + 
				            "g.timestamp = l.last_time " + 
					") AS g " + 
					"LEFT JOIN section_tables AS st ON st.table_id = g.table_id " + 
					"INNER JOIN sections AS s ON s.id = st.section_id " + 
				    "LEFT JOIN ( " + 
						"SELECT h.* FROM ( " + 
							"SELECT table_id, MAX(timestamp) AS last_time FROM table_status_history " + 
				            "WHERE NOT status = 'Check_In' " + 
				            "GROUP BY table_id) AS l " + 
						"INNER JOIN table_status_history AS h ON " + 
							"h.table_id = l.table_id AND " + 
				            "h.timestamp = l.last_time " + 
					") AS h ON h.table_id = g.table_id " + 
				    "WHERE " + 
						"s.shift_id = ? AND " + 
				        "NOT (h.status = 'Open' AND h.timestamp >= g.timestamp) AND " + 
				        "(h.status = 'Seated' OR NOT g.order_id = NULL) " + 
					"GROUP BY s.id " + 
			") AS c ON c.id = s.id");

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Shift shift = Shift.getCurrentShift();
		if(shift == null) {
			LOG.error("No shift found in the system!");
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "There is no active shift available");
		}
		
		LOG.trace("Requesting guest-count by section for Shift #{}...", shift.getId());
		
		// send item back to requester 
		JsonBuilder builder = JsonBuilder.create()
			.newArray("counts");
		
		Database.query(results -> {
			builder.newObject()
				.append("section_number", results.getInt("section"))
				.append("count", results.getInt("count"))
			.end();
		}, QUERY, shift.getId());
		
		builder.end();
		
		builder.build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "count"; }
}