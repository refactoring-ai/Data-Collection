package refactoringml.db;

import javax.persistence.*;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "FieldMetric")
public class FieldMetric extends PanacheEntity {

	@Column(nullable = true)
	public String fieldName;
	@Column(nullable = true)
	public int fieldAppearances;

	@ManyToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(nullable = false)
	public Project project;

	public FieldMetric() {
	}

	public FieldMetric(String fieldName, int fieldAppearances, Project project) {
		this.fieldName = fieldName;
		this.fieldAppearances = fieldAppearances;
		this.project = project;
	}

	@Override
	public String toString() {
		return "FieldMetric{" + "fieldName='" + fieldName + '\'' + ", fieldAppearances=" + fieldAppearances + '}';
	}
}
