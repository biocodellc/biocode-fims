
Notes about BCO data processing from NCEAS, March, 2015 meeting

***************************
* To run output in Protege
***************************

1. Convert to Turtle Format

rapper savedDataFileFromWeb.xml -i rdfxml -o turtle > data.ttl


2. Insert the following lines after prefixes (NOTE: find official location of whatever BCO version you want)(

@prefix owl: <http://www.w3.org/2002/07/owl#> .

<http://purl.obolibrary.org/obo/bco/sample.owl> rdf:type owl:Ontology ;
   owl:imports <https://raw.githubusercontent.com/tucotuco/bco/master/src/ontology/bco.owl> .


***************************
* NOCTUA Browser
***************************
Visualize Cool stuff
This is java wrapper around OwlAPI.


***************************
* Using OWL constructs to specify relationships in the data file
***************************
Currently, all explicit ontological relations are contained in the XML configuration file.
However, this duplicates what we have OWL for!
Need a way to address this.
Some thoughts here:
http://www.isi.edu/integration/karma/ (webKarma)
http://www.rightfield.org.uk/about (RightField)
   http://www.snee.com/xml/xml2006/owlrdbms.html
   http://www.dbs.cs.uni-duesseldorf.de/RDF/ (Relation.OWL)
   http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.97.5970 (DB2OWL)

***************************
* Early 2014 experiments in generating view files
***************************

The BCO configuration file currently uses many "dummy" names for classes, like "bco:morphologicalIdentificationProcess"
instead of the real URIs (like "http://purl.obolibrary.org/obo/BCO_examples_0000083")

This is so we can output results in an easy to read manner.  Eventually, we'll want to convert these to
the real URIs.

Output needs to be converted in the following ways:

rapper demo_output.ttl -i turtle -o dot > demo_output.dot

dot2vue demo_output.dot > demo_output.vue
