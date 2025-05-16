package tw.com.hyweb.cathold.sqlserver.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class MarcDetail implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -264082636234786436L;

	@Id
	private int marcId;

	private String title;

	private String author;

	private String publisher;

	private String pubyear;

	private String isbn;

}
