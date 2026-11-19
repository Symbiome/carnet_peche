<!--
  #%L
  Carnet de pêche :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
<template>
  <div class="histogram">
    <div class="histogram-chart">
      <Bar :data="chartData" :options="chartOptions" :plugins="[averageLinePlugin]" />
    </div>
    <div v-if="average" class="average">
      <span class="count">{{ average }} </span> {{ averageLabel }}
    </div>
  </div>
</template>

<script lang="ts">
import CarnetCharts from "@/services/CarnetCharts";
import { CARNET_COLORS } from "@/services/CarnetPalette";
import Constants from "@/services/Constants";

import { Component, Prop, Vue } from "vue-facing-decorator";
import { Bar } from "vue-chartjs";

import { Month, Maillage } from "@/pojos/BackendPojos";

const SERIES: { maillage: Maillage; label: string; color: string }[] = [
  { maillage: "MAILLEE", label: "Poissons maillés", color: CARNET_COLORS.sky },
  { maillage: "NON_MAILLEE", label: "Poissons non maillés", color: CARNET_COLORS.orange },
  { maillage: "NON_DEFINI", label: "Sans distinction de maillage", color: CARNET_COLORS.sky },
];

/**
 * Valeurs mensuelles par maillage (prises par mois, taille moyenne), en barres
 * Chart.js : la légende masque ou réaffiche les poissons maillés ou non maillés,
 * chaque barre porte sa valeur, le mois en cours est mis en avant et la
 * moyenne éventuelle est tracée en pointillés.
 */
@Component({
  components: { Bar },
})
export default class HistogramChart extends Vue {
  @Prop() orderedMonths: Month[];
  @Prop() values: { [P in Month]?: { [S in Maillage]: any } };
  @Prop() isActiveMonthCondition: (m: Month) => {};
  @Prop() average: number;
  @Prop() averageLabel: string;

  get series() {
    return SERIES.filter((serie) => this.orderedMonths.some((m) => this.valueOf(m, serie.maillage) > 0));
  }

  get chartData(): any {
    return {
      labels: this.orderedMonths.map((m) => this.monthLabel(m)),
      datasets: this.series.map((serie) => ({
        label: serie.label,
        data: this.orderedMonths.map((m) => Math.round(this.valueOf(m, serie.maillage))),
        backgroundColor: serie.color,
        borderRadius: 2,
      })),
    };
  }

  get chartOptions(): any {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { ...CarnetCharts.toggleableLegend("top"), display: this.series.length > 1 },
        datalabels: CarnetCharts.barValueLabels(),
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: {
            autoSkip: false,
            maxRotation: 0,
            font: (context: any) => ({ weight: this.isActiveIndex(context.index) ? "bold" : "normal" }),
            color: (context: any) => (this.isActiveIndex(context.index) ? CARNET_COLORS.orange : undefined),
          },
        },
        y: CarnetCharts.autoHiddenAxis({ beginAtZero: true, ticks: { precision: 0 } }),
      },
    };
  }

  // Moyenne en pointillés, à la place de l'ancienne ligne CSS.
  get averageLinePlugin(): any {
    return {
      id: "averageLine",
      afterDatasetsDraw: (chart: any) => {
        if (!this.average || !chart.scales.y) {
          return;
        }
        const y = chart.scales.y.getPixelForValue(this.average);
        const { left, right } = chart.chartArea;
        const context = chart.ctx;
        context.save();
        context.setLineDash([3, 3]);
        context.strokeStyle = CARNET_COLORS.orange;
        context.beginPath();
        context.moveTo(left, y);
        context.lineTo(right, y);
        context.stroke();
        context.restore();
      },
    };
  }

  valueOf(m: Month, maillage: Maillage): number {
    const monthValues = this.values && this.values[m];
    return (monthValues && Number(monthValues[maillage])) || 0;
  }

  monthLabel(m: Month): string {
    return Constants.MONTHS[this.orderedMonths.indexOf(m)];
  }

  isActiveIndex(index: number): boolean {
    return !!this.isActiveMonthCondition && !!this.isActiveMonthCondition(this.orderedMonths[index]);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.histogram {
  width: 100%;
  display: flex;
  flex-direction: column;

  .histogram-chart {
    position: relative;
    height: 250px;
  }

  .average {
    height: @average-header-height;
    line-height: @average-header-height;
    font-weight: bold;
    font-size: @fontsize-header-paragraph;
    color: @accent-text;
    margin: @margin-small auto 0 auto;
    padding-right: @margin-medium;
    display: flex;
    flex-direction: row;

    .count {
      width: 30px;
      border-radius: 15px;
      background-color: @terra-cotta;
      color: @white;
      margin-right: @margin-small;
      text-align: center;
    }
  }
}
</style>
