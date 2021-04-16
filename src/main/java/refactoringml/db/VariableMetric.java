package refactoringml.db;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "VariableMetric")
public class VariableMetric extends PanacheEntity {

	@Column(nullable = true)
	public String variableName;
	@Column(nullable = true)
	public int variableAppearances;

	@ManyToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(nullable = false)
	public Project project;

	public VariableMetric() {
	}

	public VariableMetric(String variableName, int variableAppearances, Project project) {
		this.variableName = variableName;
		this.variableAppearances = variableAppearances;
		this.project = project;
	}

	@Override
	public String toString() {
		return "VariableMetric{" + "variableName='" + variableName + '\'' + ", variableAppearances="
				+ variableAppearances + '}';
	}
}
