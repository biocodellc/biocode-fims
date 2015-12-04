package bcid.Renderer;

/**
 * HTMLTableRenderer renders Identifier results as an HTMLTable
 */
public class HTMLTableRenderer extends Renderer {

    public void enter() {
        outputSB.append("<h2>" + identifier.identifier + "</h2>\n\n");
        outputSB.append("<table>\n");
        outputSB.append("\t<tr>\n" +
                "\t\t<th>Description</th>\n" +
                "\t\t<th>Value</th>\n" +
                "\t\t<th>Definition</th>\n" +
                "\t</tr>\n");
    }

    public void printMetadata() {
        tableResourceRowAppender(resource);
        tableResourceRowAppender(about);
        tableResourceRowAppender(dcMediator);
        tableResourceRowAppender(dcHasVersion);
        tableResourceRowAppender(dcIsReferencedBy);
        tableResourceRowAppender(dcRights);
        tableResourceRowAppender(dcIsPartOf);
        tablePropertyRowAppender(dcDate);
        tablePropertyRowAppender(dcCreator);
        tablePropertyRowAppender(dcTitle);
        tablePropertyRowAppender(dcSource);
        tablePropertyRowAppender(bscSuffixPassthrough);
    }

    public void leave() {
        outputSB.append("</table>\n");
    }

    public boolean validIdentifier() {
        if (identifier == null) {
            outputSB.append("<h2>Unable to find identifier</h2>");
            return false;
        } else {
            return true;
        }
    }

    /**
     * append each property
     *
     * @param map
     */
    private void tablePropertyRowAppender(metadataElement map) {
        if (map != null) {
            if (!map.getValue().trim().equals("")) {
                outputSB.append("\t<tr>\n" +
                        "\t\t<td>" + map.getValue() + "</td>\n" +
                        "\t\t<td><a href=\"" + map.getFullKey() + "\">" + map.getKey() + "</a></td>\n" +
                        "\t\t<td>" + map.getDescription() + "</td>\n" +
                        "\t</tr>\n");
            }
        }
    }

    /**
     * append each property
     *
     * @param map
     */
    private void tableResourceRowAppender(metadataElement map) {
        if (map != null) {
            if (!map.getValue().trim().equals("")) {
                outputSB.append("\t<tr>\n" +
                        "\t\t<td><a href=\"" + map.getValue() + "\">" + map.getValue() + "</a></td>\n" +
                        "\t\t<td><a href=\"" + map.getFullKey() + "\">" + map.getKey() + "</a></td>\n" +
                        "\t\t<td>" + map.getDescription() + "</td>\n" +
                        "\t</tr>\n");
            }
        }

    }
}
