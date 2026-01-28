import axios from 'axios';

// /rest/plugin/<plugin key>
export const baseURL = '/rest/plugin/ru.slie.luna.templates.demo-addon';
export const client = axios.create({baseURL});