The BCO configuration file currently uses many "dummy" names for classes, like "bco:morphologicalIdentificationProcess"
instead of the real URIs (like "http://purl.obolibrary.org/obo/BCO_examples_0000083")

This is so we can sketch out, output results in an easy to read manner.  Eventually, we'll want to convert these to
the real URIs.

Output needs to be converted in the following ways:

rapper demo_output.ttl -i turtle -o dot > demo_output.dot

dot2vue demo_output.dot > demo_output.vue