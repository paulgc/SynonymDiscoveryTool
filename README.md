# Synonym Discovery Tool

An interactive tool to help users find synonyms to expand an regular expression. 

For example, given a simple regex such as "phone:\d{10}" and the user wants to find synonyms for the phrase "phone", the user 
provides a modified regex such as "(phone|\syn):\d{10}" to the tool. The tag "\syn" indicates that synonyms need to be found for
the phrase "phone". The tool takes this modified regex and data as input, and returns candidate synonyms such as "mobile",
"telephone", "cell", "home", "office", "tel", "cellphone" etc. 

Each time the top 10 candidates are shown to the user and the user is asked to provide feedback. Based on the feedback, the next top 10
candidates is shown to the user.
