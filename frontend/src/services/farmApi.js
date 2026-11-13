import { initialFarmState } from '../reducers/farmReducer';

export async function getFarmSummary() {
  return Promise.resolve(initialFarmState);
}
