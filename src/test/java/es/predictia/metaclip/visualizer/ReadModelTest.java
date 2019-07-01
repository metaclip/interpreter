package es.predictia.metaclip.visualizer;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReadModelTest {

	@Test
	public void testRead() throws Exception{
		OntModel ontModel = ModelFactory.createOntologyModel();
		ontModel.read("http://www.metaclip.org/graphical_output/graphical_output.owl");
	}
}
