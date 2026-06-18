import axios from 'axios';
import { ResponseError } from "luna";

// /rest/plugin/<plugin key>
export const baseURL = '/rest/plugin/ru.slie.luna.template.demo-addon';
const axiosClient = axios.create({baseURL});
axiosClient.interceptors.response.use(
    function(response) {
      return response;
    },
    function(error) {
      if (axios.isAxiosError<ResponseError>(error) && error.response) {
        return Promise.reject(error.response);
      }

      return Promise.reject({
        status: error.status,
        reason: error.statusText
      } as ResponseError);
    }
);

export const client = axiosClient;

// example:
//
// client.get<MyObject>('/my_prefix/objects/1').then((data) => {
//  console.log(data.data); // MyObject
// }).catch((e) => {
//  console.log(e.data?.errors); // object response from error
// })
