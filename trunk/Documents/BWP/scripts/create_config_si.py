import unicodecsv
from lxml import etree
from xml.dom import minidom
from collections import OrderedDict

config_output = "../config/si_config.xml"
bwp_file = "../terms/terms_6_19.tsv"
si_file = "../terms/si_category_order_6_16.tsv"

si_terms = []
si_required = []
with open(si_file, 'rb') as csvfile:
	reader = unicodecsv.DictReader(csvfile, delimiter="\t",encoding='utf-8')
	for row in reader:
		si_terms.append(row['Term Name'])
		if row['Require Level'] == 'required':
			si_required.append(row['Term Name'])

bwp_terms = OrderedDict()
with open(bwp_file, 'rb') as csvfile:
	reader = unicodecsv.DictReader(csvfile, delimiter="\t",encoding='utf-8')
	for row in reader:
		term_dict = {}
		term_name = row['Term Name']
		row.pop('Term Name')
		for key in row:
			if len(row[key]) > 1: #Check to make sure that there is a value in that cell
				term_dict[key] = row[key].encode('ascii','xmlcharrefreplace') #This converts any unicode characters to htmlentities
		bwp_terms[term_name] = term_dict

fims_xml = etree.Element('fims')
metadata = etree.SubElement(fims_xml,'metadata')
metadata.set('doi','a doi')
metadata.set('shortname','SIBarcodingCBOL')
metadata.set('eml_location','eml_location')
metadata.set('target','http://data.biscicol.org/ds/data')
metadata.set('queryTarget','http://data.biscicol.org/ds')
metadata.text = etree.CDATA("Fill this in later.")

mapping = etree.SubElement(fims_xml,'mapping')
entity = etree.SubElement(mapping,'entity')
entity.set('worksheet','Samples')
entity.set('worksheetUniqueKey','voucherID')
entity.set('conceptAlias','Resource')
entity.set('conceptURI','http://www.w3.org/2000/01/rdf-schema#Resource')
entity.set('entityID','1')

validation = etree.SubElement(fims_xml,'validation')
worksheet = etree.SubElement(validation,'worksheet')
worksheet.set('sheetname','Samples')

rule = etree.SubElement(worksheet,'rule')
rule.set('type','duplicateColumnNames')
rule.set('level','error')

rule = etree.SubElement(worksheet,'rule')
rule.set('type','uniqueValue')
rule.set('column','voucherID')
rule.set('level','error')


req_error = etree.SubElement(worksheet,'rule')
req_error.set('type','RequiredColumns')
req_error.set('column','RequiredColumns')
req_error.set('level','error')

for term in si_terms:
	attribute = etree.SubElement(entity,'attribute')
	attribute.set('column',term)
	attribute.set('uri', 'urn:'+term)
	if term in bwp_terms:
		if 'DwC Term' in bwp_terms[term]:
			attribute.set('defined_by',bwp_terms[term]['DwC Term'])
		if 'Definition' in bwp_terms[term]:
			attribute.text = etree.CDATA(bwp_terms[term]['Definition'])

for req in si_required:
	error_field = etree.SubElement(req_error,'field')
	error_field.text = req

target = open(config_output, 'w')
target.write("<?xml version='1.0' encoding='UTF-8'?>"+"\n")

target.write(etree.tostring(fims_xml, pretty_print=True))


