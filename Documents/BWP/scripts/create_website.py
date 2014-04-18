import unicodecsv
from collections import OrderedDict

latest_term_file = "../terms/terms_4_16.tsv"
latest_list_file = "../terms/lists_4_16.tsv"
website_header = "../term_site/terms_header.html"
website_footer = "../term_site/terms_footer.html"
website_output = "../term_site/terms_4_17.html"



bwp_terms = OrderedDict()

with open(latest_term_file, 'rb') as csvfile:
	reader = unicodecsv.DictReader(csvfile, delimiter="\t",encoding='utf-8')
	for row in reader:
		term_dict = OrderedDict()
		term_name = row['Term Name']
		row.pop('Term Name')
		for key in row:
			if len(row[key]) > 1: #Check to make sure that there is a value in that cell
				term_dict[key] = row[key].encode('ascii','xmlcharrefreplace') #This converts any unicode characters to htmlentities
		bwp_terms[term_name] = term_dict

list_dict = OrderedDict()
list_description = {}
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
				list_entry = list_entry.replace("<","&lt;")
				list_entry = list_entry.replace(">","&gt;")
				list_dict[key].append(list_entry)

with open(website_output, 'w') as outfile:
	with open(website_header,'r') as infile:
		for line in infile:
			outfile.write(line)
	for term in bwp_terms:
		outfile.write("<div class='row'>\n")
		outfile.write("\t<div class='col-md-6 col-md-offset-3'>\n")
		if 'FIMS Level' in bwp_terms[term]:
			if bwp_terms[term]['FIMS Level'] == 'error':
				outfile.write("\t\t<div class='panel required'>\n")
			else:
				outfile.write("\t\t<div class='panel recommended'>\n")	
		else:
			outfile.write("\t\t<div class='panel'>\n")
		outfile.write("\t\t\t<div class='panel-heading'>\n" )
		outfile.write("\t\t\t\t<span class='expand_contract glyphicon glyphicon-plus-sign'></span>%s" % term )

		if 'FIMS Level' in bwp_terms[term]:
			if bwp_terms[term]['FIMS Level'] == 'error':
				outfile.write("<span class='pull-right glyphicon glyphicon-exclamation-sign'></span>")
			else:
				outfile.write("<span class='pull-right glyphicon glyphicon-warning-sign'></span>")		

		if 'List Name' in bwp_terms[term]:
			outfile.write("<span class='pull-right glyphicon glyphicon-list-alt'></span>")

		outfile.write("\n")
		outfile.write("\t\t\t</div>\n")
		outfile.write("\t\t\t<div class='panel-body panel-collapsed'>\n")
		outfile.write("\t\t\t\t<p><span class='term_key'>Category: </span>%s</p>\n" % bwp_terms[term]['Category'])
		if 'DwC Term' in bwp_terms[term]:
			outfile.write("\t\t\t\t<p><span class='term_key'>DarwinCore Equivalent: </span><a href='%s' target='_blank'>%s</a></p>\n" % (bwp_terms[term]['DwC Term'],bwp_terms[term]['DwC Term']))
		if 'Genbank Term' in bwp_terms[term]:
			outfile.write("\t\t\t\t<p><span class='term_key'>GenBank Equivalent: </span>%s</p>\n" % bwp_terms[term]['Genbank Term'])
		outfile.write("\t\t\t\t<p><span class='term_key'>Definition: </span>%s</p>\n" % bwp_terms[term]['Definition'])
		if 'Example' in bwp_terms[term]:
			outfile.write("\t\t\t\t<p><span class='term_key'>Example: </span>%s</p>\n" % bwp_terms[term]['Example'])
		if 'List Name' in bwp_terms[term]:
			if bwp_terms[term]['List Level'] == 'warning':
				list_level = "<em>Warning</em>"
			elif bwp_terms[term]['List Level'] == 'error':
				list_level = "<strong>Error</strong>"

			outfile.write("\t\t\t\t<p><span class='term_key'>Controlled Vocabulary: </span>%s if not in <a href='#%sList'  data-toggle='modal'>%s List</a></p>\n" % (list_level,bwp_terms[term]['List Name'],bwp_terms[term]['List Name']))
		outfile.write("\t\t\t</div>\n")
		outfile.write("\t\t</div>\n")
		outfile.write("\t</div>\n")
		outfile.write("</div>\n")

	for list_name in list_dict:
		outfile.write("<div class='modal fade' id='%sList'>\n" % list_name)
		outfile.write("\t<div class='modal-dialog modal-sm'>\n")
		outfile.write("\t\t<div class='modal-content'>\n")
		outfile.write("\t\t\t<div class='modal-header'>\n")
		outfile.write("\t\t\t\t<button type='button' class='close' data-dismiss='modal' aria-hidden='true'>&times;</button>\n")
		outfile.write("\t\t\t\t<h4 class='modal-title'>Controlled vocabulary</h4>\n")
		outfile.write("\t\t\t</div>\n")
		outfile.write("\t\t\t<div class='modal-body'>\n")
		outfile.write("\t\t\t\t<p>%s</p>\n" % list_description[list_name])
		if list_name != 'institutionCode' and list_name != 'year':
			outfile.write("\t\t\t\t<ul>\n")
			for list_value in list_dict[list_name]:
				outfile.write("\t\t\t\t\t<li>%s</li>\n" % list_value)
			outfile.write("\t\t\t\t</ul>\n")
		outfile.write("\t\t\t</div>\n")
		outfile.write("\t\t\t<div class='modal-footer'>\n")
		outfile.write("\t\t\t\t<button type='button' class='btn btn-default' data-dismiss='modal'>Close</button>\n")
		outfile.write("\t\t\t</div>\n")
		outfile.write("\t\t</div>\n")
		outfile.write("\t</div>\n")
		outfile.write("</div>\n")

	with open(website_footer,'r') as infile:
		for line in infile:
			outfile.write(line)	

