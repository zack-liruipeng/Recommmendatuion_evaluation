library(dplyr)
library(ggplot2)
options(repr.plot.width=5, repr.plot.height=4)
results = read.csv('eval-results.csv')
head(results)
static_results = results %>% filter(is.na(NNbrs))
head(static_results)
ggplot(static_results) +
    aes(x=Algorithm, y=RMSE.ByUser) +
    geom_boxplot()