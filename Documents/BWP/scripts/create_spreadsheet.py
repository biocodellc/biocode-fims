import unicodecsv
import xlsxwriter
from collections import OrderedDict

latest_term_file = "../terms/terms_5_08.tsv"
spreadsheet_output = "../spreadsheet/BWP_FIMS_Spreadsheet_8_May_14.xlsx"

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

workbook = xlsxwriter.Workbook(spreadsheet_output)
worksheet = workbook.add_worksheet('Samples')

required = workbook.add_format({'bg_color':'#A4C2F4', 'bold':True})
recommended = workbook.add_format({'bg_color':'#FCE5CD','bold':True})
optional = workbook.add_format({'bold':True})


row = 0
col = 0
for term in bwp_terms:
	if 'FIMS Level' in bwp_terms[term]:
		if bwp_terms[term]['FIMS Level'] == 'error':
			worksheet.write(row,col,term,required)
		else:
			worksheet.write(row,col,term,recommended)
	else:
		worksheet.write(row,col,term,optional)
	worksheet.set_column(col,col,len(term)+1)
	if 'Definition' in bwp_terms[term]:
		worksheet.write_comment(row,col,bwp_terms[term]['Definition'])
	col += 1

workbook.close()


