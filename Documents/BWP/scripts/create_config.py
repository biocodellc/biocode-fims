import unicodecsv
from lxml import etree
from xml.dom import minidom
from collections import OrderedDict

latest_term_file = "../terms/terms_6_19.tsv"
latest_list_file = "../terms/lists_4_16.tsv"
config_output = "../config/bwp_config.xml"

bwp_terms = OrderedDict()
with open(latest_term_file, 'rb') as csvfile:
	reader = unicodecsv.DictReader(csvfile, delimiter="\t",encoding='utf-8')
	for row in reader:
		term_dict = {}
		term_name = row['Term Name']
		row.pop('Term Name')
		for key in row:
			if len(row[key]) > 1: #Check to make sure that there is a value in that cell
				term_dict[key] = row[key].encode('ascii','xmlcharrefreplace') #This converts any unicode characters to htmlentities
		bwp_terms[term_name] = term_dict

list_dict = OrderedDict()
list_description = OrderedDict()
with open(latest_list_file, 'rb') as csvfile:
	reader = unicodecsv.DictReader(csvfile, delimiter="\t",encoding='utf-8')
	descriptions = reader.next()
	for key in descriptions:
		if key != 'Term':
			list_description[key] = descriptions[key]
			list_dict[key] = []
	for row in reader:
		for key in row:
			if len(row[key]) and key != 'Term':
				list_entry = row[key].encode('ascii','xmlcharrefreplace')
				list_dict[key].append(list_entry)

fims_xml = etree.Element('fims')
metadata = etree.SubElement(fims_xml,'metadata')
metadata.set('doi','a doi')
metadata.set('shortname','BarcodeOfWildlife')
metadata.set('eml_location','eml_location')
metadata.set('target','http://data.biscicol.org/ds/data')
metadata.set('queryTarget','http://data.biscicol.org/ds')
metadata.text = etree.CDATA("The Barcode of Wildlife Project is an initiative run by the Consortium for the Barcode of Life to use standardized DNA sequences to identify and protect endangered species.")

mapping = etree.SubElement(fims_xml,'mapping')
entity = etree.SubElement(mapping,'entity')
entity.set('worksheet','Samples')
entity.set('worksheetUniqueKey','tissueBarcode')
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
rule.set('column','tissueBarcode')
rule.set('level','error')


req_error = etree.SubElement(worksheet,'rule')
req_error.set('type','RequiredColumns')
req_error.set('column','RequiredColumns')
req_error.set('level','error')

req_warning = etree.SubElement(worksheet,'rule')
req_warning.set('type','RequiredColumns')
req_warning.set('column','RequiredColumns')
req_warning.set('level','warning')


for term in bwp_terms:
	attribute = etree.SubElement(entity,'attribute')
	attribute.set('column',term)
	attribute.set('uri', 'urn:'+term)
	if 'DwC Term' in bwp_terms[term]:
		attribute.set('defined_by',bwp_terms[term]['DwC Term'])
	if 'Definition' in bwp_terms[term]:
		attribute.text = etree.CDATA(bwp_terms[term]['Definition'])
	if 'FIMS Level' in bwp_terms[term]:
		if bwp_terms[term]['FIMS Level'] == 'error':
			error_field = etree.SubElement(req_error,'field')
			error_field.text = term
		elif bwp_terms[term]['FIMS Level'] == 'warning':
			warning_field = etree.SubElement(req_warning,'field')
			warning_field.text = term
	if 'List Name' in bwp_terms[term]:
		list_rule = etree.SubElement(worksheet,'rule')
		list_rule.set('type','checkInXMLFields')
		list_rule.set('column',term)
		list_rule.set('list',bwp_terms[term]['List Name'])
		list_rule.set('level',bwp_terms[term]['List Level'])

lists = etree.SubElement(validation,'lists')

for list_name in list_dict:
	field_list = etree.SubElement(lists,'list')
	field_list.set('alias',list_name)
	for field in list_dict[list_name]:
		list_field = etree.SubElement(field_list,'field')
		list_field.text = field

target = open(config_output, 'w')
target.write("<?xml version='1.0' encoding='UTF-8'?>"+"\n")

target.write(etree.tostring(fims_xml, pretty_print=True))


